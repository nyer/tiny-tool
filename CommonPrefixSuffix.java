/**
 * get max common suffix and prefix of two strings through binary-search.
 * @author ls5231#gmail.com
 *
 */
public class CommonPrefixSuffix {
    /**
     * return the end location of max common prefix.
     * @param text1
     * @param text2
     */
    public static int commonPrefix(String text1, String text2) {
        //quick check
        if (text1.length() == 0 || text2.length() == 0 || text1.charAt(0) != text2.charAt(0)) {
            return 0;
        }
        
        int min = 0;
        int max = Math.min(text1.length(), text2.length());
        int mid = max;
        
        while (min != max && min != mid) {
            if (text1.regionMatches(min, text2, min, mid - min)) {
                min = mid;
            } else {
                max = mid;
            }
            mid = (max - min)/2 + min;
        }
        return mid;
    }
    
    /**
     * code from google's diff_match_patch.
     * @param text1
     * @param text2
     * @return
     */
    public static int diff_commonPrefix(String text1, String text2) {  
        // Quick check for common null cases.  
        if (text1.length() == 0 || text2.length() == 0 ||  
            text1.charAt(0) != text2.charAt(0)) {  
          return 0;  
        }  
        // Binary search.  
        int pointermin = 0;  
        int pointermax = Math.min(text1.length(), text2.length());  
        int pointermid = pointermax;  
        int pointerstart = 0;  
        while (pointermin < pointermid) {  
          if (text1.regionMatches(pointerstart, text2, pointerstart,  
              pointermid - pointerstart)) {  
            pointermin = pointermid;  
            pointerstart = pointermin;  
          } else {  
            pointermax = pointermid;  
          }  
          pointermid = (pointermax - pointermin) / 2 + pointermin;  
        }  
        return pointermid;  
      }  
    

    /**
     * return the length of max common suffix.
     * @param text1
     * @param text2
     */
    public static int commonSuffix(String text1, String text2) {
        if (text1.length() == 0 || text2.length() == 0 
                        || text1.charAt(text1.length() -1) != text2.charAt(text2.length() -1) ) {
            return 0;
        }
        
        int min = - Math.min(text1.length(), text2.length());
        int mid = min;
        int max = -1;
        
        int t1l = text1.length();
        int t2l = text2.length();
        while (max != min && max != mid) {
            if (text1.regionMatches(t1l + mid, text2, t2l + mid, max - mid + 1)) {
                max = mid;
            } else {
                min = mid;
            }
            mid = (int)Math.ceil((max - min) / 2.0) + min;
        }
        return Math.abs(mid);
    }
    
    /**
     * code from google's diff_match_patch.
     * @param text1
     * @param text2
     * @return
     */
    public static int diff_commonSuffix(String text1, String text2) {  
        // Quick check for common null cases.  
        if (text1.length() == 0 || text2.length() == 0 ||  
            text1.charAt(text1.length() - 1) != text2.charAt(text2.length() - 1)) {  
          return 0;  
        }  
        // Binary search.  
        int pointermin = 0;  
        int pointermax = Math.min(text1.length(), text2.length());  
        int pointermid = pointermax;  
        int pointerend = 0;  
        while (pointermin < pointermid) {  
          if (text1.regionMatches(text1.length() - pointermid, text2,  
                                  text2.length() - pointermid,  
                                  pointermid - pointerend)) {  
            pointermin = pointermid;  
            pointerend = pointermin;  
          } else {  
            pointermax = pointermid;  
          }  
          pointermid = (pointermax - pointermin) / 2 + pointermin;  
        }  
        return pointermid;  
      }  
    
    public static void main(String[] args) {
        String text1 = "1s243231";
        String text2 = "1243231";
        System.out.println(commonPrefix(text1, text2) == diff_commonPrefix(text1, text2));
        System.out.println(commonSuffix(text1, text2) == diff_commonSuffix(text1, text2));
    }
}
