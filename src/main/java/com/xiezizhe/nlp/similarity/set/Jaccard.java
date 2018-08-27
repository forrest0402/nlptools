package forrest.algorithm.nlp.similarity.set;

import com.xiezizhe.nlp.similarity.DistanceFunction;
import com.xiezizhe.nlp.similarity.Similarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author forrest0402
 * @Description
 * @date 2018/6/28
 */
public class Jaccard<T> implements Similarity<T> {

    private static Logger logger = LoggerFactory.getLogger(Jaccard.class);

    public double dist(List<T> T1, List<T> T2) {
        double ans = 0;
        Set<T> intersection = new HashSet<T>(T2), union = new HashSet<T>(T2);
        for (T t1 : T1) {
            union.add(t1);
            if (intersection.contains(t1)) {
                ++ans;
            }
        }
        return ans / union.size();
    }
}
