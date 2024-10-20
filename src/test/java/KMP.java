import java.util.Arrays;

/**
 * @CreateTime: 2024-10-19  22:51
 * @Author: 小汉同学
 * @Description:
 */
public class KMP {
    public static void main(String[] args) {
        String s = "abcdefg";
        String p = "cde";
        String sp = p + "#" + s;
        int[] pi = new int[sp.length()];
        char[] array = sp.toCharArray();
        for (int i = 1; i < array.length; i++) {
            int len = pi[i - 1];
            while (len != 0 && array[i] != array[len]) {
                len = pi[len - 1];
            }
            if (array[i] == array[len]) {
                pi[i] = len + 1;
            }
        }

        System.out.println(Arrays.toString(pi));
    }
}
