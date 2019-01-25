package com.xiezizhe.nlp.utils;

import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by xiezizhe
 * Date: 2018/8/16 下午7:54
 */
@Service
public class ExcelUtils {

    private static Logger logger = LoggerFactory.getLogger(ExcelUtils.class);

    public final static int CATEGORY_IDX = 0;
    public final static int PARENT_CATEGORY_IDX = 1;
    public final static int CATEGORY_NAME_IDX = 2;
    public final static int STAND_QUES_IDX = 3;
    public final static int SIM_QUES_IDX = 4;
    public final static int ANSWER_IDX = 5;
    public final static int ANSWER_TYPE_IDX = 6;
    public final static int EXPIRE_DATE_IDX = 7;
    public final static int OTHER_ANSWER_IDX = 8;
    public final static int EFFECT_IDX = 9;

    @Autowired
    Environment environment;

    public static final Set<String> FILTERED_STANDARD_QUES = new HashSet<>();


    public static final String[] TITLE = new String[]{"目录ID(整数,选填)",
            "父目录ID(根目录的父目录为空)",
            "目录名称",
            "标准问(必填,当目录信息非空时,问答信息可为空,下同)",
            "相似问(选填,多个在同一单元格中用换行分开)",
            "通用答案(json,如{\"text\":\"晴转多云\"},必填)",
            "答案类型(1-文本/2-链接,必填)",
            "过期时间(格式如：2099-12-31 23:59:59,选填)",
            "其它渠道答案(json数组,如:[{\"answer\":{\"text\":\"欢迎下载南京银行APP查询\"},\"type\":1,\"channel\":\"APP\"}," +
                    "{\"answer\":{\"text\":\"欢迎关注微信公众号njyh\"},\"type\":1,\"channel\":\"微信\"}],选填)",
            "生效时间(格式如：2000-01-01 00:00:00,选填)"};

    public Map<String, Set<String>> getFaq(String fileName) throws IOException {
        String[][] excelFile = getData(fileName, 1);
        Map<String, Set<String>> faqs = new HashMap<>();
        for (int i = 0; i < excelFile.length; i++) {
            String[] row = excelFile[i];
            faqs.putIfAbsent(row[STAND_QUES_IDX], new HashSet<>());
            String[] simQuestions = excelFile[i][SIM_QUES_IDX].split("\n");
            faqs.get(row[STAND_QUES_IDX]).addAll(Arrays.asList(simQuestions));
        }
        return faqs;
    }

    public String[][] getData(String fileName, int ignoreRows) throws IOException {
        return getData(new File(fileName), ignoreRows);
    }

    /**
     * 读取Excel的内容，第一维数组存储的是一行中格列的值，二维数组存储的是多少个行
     *
     * @param file       读取数据的源Excel
     * @param ignoreRows 读取数据忽略的行数，比喻行头不需要读入 忽略的行数为1
     * @return 读出的Excel中数据的内容
     * @throws FileNotFoundException
     * @throws IOException
     */
    public String[][] getData(File file, int ignoreRows)
            throws FileNotFoundException, IOException {
        List<String[]> result = new ArrayList<String[]>();
        int rowSize = 0;
        XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream(file));
        XSSFCell cell = null;
        for (int sheetIndex = 0; sheetIndex < wb.getNumberOfSheets(); sheetIndex++) {
            XSSFSheet st = wb.getSheetAt(sheetIndex);
            // 第一行为标题，不取
            for (int rowIndex = ignoreRows; rowIndex <= st.getLastRowNum(); rowIndex++) {
                XSSFRow row = st.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                int tempRowSize = row.getLastCellNum() + 1;
                if (tempRowSize > rowSize) {
                    rowSize = tempRowSize;
                }
                String[] values = new String[rowSize];
                Arrays.fill(values, "");
                boolean hasValue = false;
                for (short columnIndex = 0; columnIndex <= row.getLastCellNum(); columnIndex++) {
                    String value = "";
                    cell = row.getCell(columnIndex);
                    if (cell != null) {
                        // 注意：一定要设成这个，否则可能会出现乱码,后面版本默认设置
                        //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
                        switch (cell.getCellType()) {
                            case HSSFCell.CELL_TYPE_STRING:
                                //value = cell.getStringCellValue();
                                value = cell.getRichStringCellValue().getString();
                                break;
                            case HSSFCell.CELL_TYPE_NUMERIC:
                                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                                    Date date = cell.getDateCellValue();
                                    if (date != null) {
                                        value = new SimpleDateFormat("yyyy-MM-dd")
                                                .format(date);
                                    } else {
                                        value = "";
                                    }
                                } else {
                                    value = new DecimalFormat("0").format(cell.getNumericCellValue());
                                }
                                break;
                            case HSSFCell.CELL_TYPE_FORMULA:
                                // 导入时如果为公式生成的数据则无值
                                if (!cell.getStringCellValue().equals("")) {
                                    value = cell.getStringCellValue();
                                } else {
                                    value = cell.getNumericCellValue() + "";
                                }
                                break;
                            case HSSFCell.CELL_TYPE_BLANK:
                                break;
                            case HSSFCell.CELL_TYPE_ERROR:
                                value = "";
                                break;
                            case HSSFCell.CELL_TYPE_BOOLEAN:
                                value = (cell.getBooleanCellValue() == true ? "Y" : "N");
                                break;
                            default:
                                value = "";
                        }
                        //value = URLEncoder.encode(value, "GBK");
                    }
                    if (columnIndex == 0 && value.trim().equals("")) {
                        break;
                    }
                    values[columnIndex] = rightTrim(value);
                    hasValue = true;
                }
                if (hasValue) {
                    result.add(values);
                }
            }
        }
        String[][] returnArray = new String[result.size()][rowSize];
        int totoalLines = 0;
        for (int i = 0; i < returnArray.length; i++) {
            returnArray[i] = result.get(i);
            totoalLines += returnArray[i][SIM_QUES_IDX] == null ? 0 : returnArray[i][SIM_QUES_IDX].split("\n").length;
        }
        logger.info("{} has {} standard questions, {} similar questions",
                file.getName().replace(".xlsx", ""), returnArray.length, totoalLines);
        return returnArray;
    }


    /**
     * 导出Excel
     *
     * @param sheetName sheet名称
     * @param values    内容
     * @param wb        HSSFWorkbook对象
     * @return
     */
    public XSSFWorkbook getXSSFWorkbook(String sheetName, String[][] values, XSSFWorkbook wb) {

        // 第一步，创建一个HSSFWorkbook，对应一个Excel文件
        if (wb == null) {
            wb = new XSSFWorkbook();
        }

        // 第二步，在workbook中添加一个sheet,对应Excel文件中的sheet
        XSSFSheet sheet = wb.createSheet(sheetName);

        // 第三步，在sheet中添加表头第0行,注意老版本poi对Excel的行数列数有限制
        XSSFRow row = sheet.createRow(0);

        // 第四步，创建单元格，并设置值表头 设置表头居中
        XSSFCellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER); // 创建一个居中格式

        //声明列对象
        XSSFCell cell = null;

        //创建标题
        for (int i = 0; i < TITLE.length; i++) {
            cell = row.createCell(i);
            cell.setCellValue(TITLE[i]);
            cell.setCellStyle(style);
        }

        //创建内容
        for (int i = 0; i < values.length; i++) {
            row = sheet.createRow(i + 1);
            for (int j = 0; j < values[i].length; j++) {
                //将内容按顺序赋给对应的列对象
                row.createCell(j).setCellValue(values[i][j]);
            }
        }

        return wb;
    }

    /**
     * @param faq
     */
    public void writeFaqToFile(Map<String, Set<String>> faq, String filePath) throws IOException {

        // 第一步，创建一个HSSFWorkbook，对应一个Excel文件
        XSSFWorkbook wb = new XSSFWorkbook();

        // 第二步，在workbook中添加一个sheet,对应Excel文件中的sheet
        XSSFSheet sheet = wb.createSheet("1");

        // 第三步，在sheet中添加表头第0行,注意老版本poi对Excel的行数列数有限制
        XSSFRow row = sheet.createRow(0);

        // 第四步，创建单元格，并设置值表头 设置表头居中
        XSSFCellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER); // 创建一个居中格式

        //声明列对象
        XSSFCell cell = null;

        //创建标题
        for (int i = 0; i < TITLE.length; i++) {
            cell = row.createCell(i);
            cell.setCellValue(TITLE[i]);
            cell.setCellStyle(style);
        }

        //创建内容
        int rowIdx = 1;
        for (Map.Entry<String, Set<String>> entry : faq.entrySet()) {
            List<String> questions = entry.getValue().stream().distinct().collect(Collectors.toList());
            if (questions != null && questions.size() > 0) {
                for (int j = 0; j < questions.size(); j += 500) {
                    row = sheet.createRow(rowIdx);
                    insertRow(row, rowIdx, entry.getKey());
                    row.createCell(SIM_QUES_IDX)
                            .setCellValue(new HashSet<>(questions.subList(j, Math.min(j + 500, questions.size())))
                                    .stream().collect(Collectors.joining("\n")));
                    ++rowIdx;
                }
            }
        }
        wb.write(new FileOutputStream(filePath));
    }

    private void insertRow(XSSFRow row, int rowIdx, String standQues) {
        row.createCell(CATEGORY_IDX).setCellValue(rowIdx);
        row.createCell(PARENT_CATEGORY_IDX).setCellValue(0);
        row.createCell(CATEGORY_NAME_IDX).setCellValue("默认");
        row.createCell(STAND_QUES_IDX).setCellValue(standQues);
        row.createCell(ANSWER_IDX).setCellValue("{\"text\":\"无答案啦\"}");
        row.createCell(ANSWER_TYPE_IDX).setCellValue("1");
    }

    /**
     * 去掉字符串右边的空格
     *
     * @param str 要处理的字符串
     * @return 处理后的字符串
     */

    private String rightTrim(String str) {
        if (str == null) {
            return "";
        }
        int length = str.length();
        for (int i = length - 1; i >= 0; i--) {
            if (str.charAt(i) != 0x20) {
                break;
            }
            length--;
        }
        return str.substring(0, length);
    }


    private String trimSentence(String sentence) {
        return sentence.replaceAll("[\r\t?？]", "").replace("\"", "\\\"")
                .replaceAll("[。？?!！]", "").trim();
    }


    /**
     * 稍微整理一下excel中的句子，去掉奇怪的引号、和\r符号还有多余的whitespace，还有句号，感叹号，问号
     *
     * @param excelFile
     * @return
     */
    public Map<String, Set<String>> trimFAQFile(String[][] excelFile) {
        return trimFAQFile(excelFile, false);
    }

    /**
     * 稍微整理一下excel中的句子，去掉奇怪的引号、和\r符号还有多余的whitespace，还有句号，感叹号，问号
     *
     * @param excelFile
     * @return key是标准问，value是扩展问（包括标准问）
     */
    public Map<String, Set<String>> trimFAQFile(String[][] excelFile, boolean filter) {
        Map<String, String> duplicateSimSentence = new HashMap<>();
        Set<String> stopSentence = new HashSet<>(Arrays.asList(environment.getProperty("stop.sentence", "")
                .split(",")));
        Map<String, Set<String>> ans = new HashMap<>();
        for (int i = 0; i < excelFile.length; ++i) {
            String standQuestion = trimSentence(excelFile[i][STAND_QUES_IDX]);
            Set<String> simQuestions = ans.get(standQuestion);
            if (simQuestions == null) {
                simQuestions = new HashSet<>();
                ans.put(standQuestion, simQuestions);
                simQuestions.add(standQuestion);
            }
            String[] array = excelFile[i][SIM_QUES_IDX].split("\n");
            List<String> trimmed = Arrays.asList(array).stream()
                    .map(c -> trimSentence(c))
                    .filter(c -> "" != c && c.length() > 2 && !stopSentence.contains(c))
                    .collect(Collectors.toList());
            if (filter) {
                trimmed = trimmed.stream().map(s -> ToAnalysis.parse(s).getTerms()
                        .stream().map(Term::getName).filter(t -> !NlpUtils.isDigit(t))
                        .collect(Collectors.joining("")))
                        .filter(s -> s.length() > 2).collect(Collectors.toList());
            }

            Iterator<String> iter = trimmed.listIterator();
            while (iter.hasNext()) {
                String simQuestion = iter.next();
                if (duplicateSimSentence.containsKey(simQuestion)) {
                    if (standQuestion.equals(duplicateSimSentence.get(simQuestion)))
                        logger.info("{} appears in previous standard question '{}', its current standard question is " +
                                        "'{}', please add this word to stop-sentence", simQuestion,
                                duplicateSimSentence.get(simQuestion), standQuestion);
                    else {
                        logger.error("{} appears in previous standard question '{}', its current standard question is" +
                                        "'{}', please add this word to stop-sentence", simQuestion,
                                duplicateSimSentence.get(simQuestion), standQuestion);
                    }
                    iter.remove();
                } else {
                    duplicateSimSentence.put(simQuestion, standQuestion);
                }
            }

            simQuestions.addAll(trimmed);

            if (simQuestions.size() < 2)
                ans.remove(standQuestion);
        }

        File filterFile = new File("standard_questions.txt");
        if (filterFile.exists()) {
            try {
                FILTERED_STANDARD_QUES.addAll(Files.lines(Paths.get(filterFile.getPath())).collect(Collectors.toSet()));
            } catch (IOException e) {
                logger.error("", e);
            }
        }
        return ans;
    }

    private String[][] faqToExcel(Map<String, Set<String>> faq) {
        String[][] excel = new String[faq.size() + 1][TITLE.length];
        int idx = 0;
        for (Map.Entry<String, Set<String>> entry : faq.entrySet()) {
            excel[idx][STAND_QUES_IDX] = entry.getKey();
            excel[idx][SIM_QUES_IDX] = entry.getValue().stream().collect(Collectors.joining("\n"));
            ++idx;
        }
        return excel;
    }


    public static final double DUP_THRESHOLD = 0;

}
