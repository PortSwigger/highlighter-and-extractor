package hae;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Config {
    public static String suffix = "3g2|3gp|7z|aac|abw|aif|aifc|aiff|apk|arc|au|avi|azw|bat|bin|bmp|bz|bz2|cmd|cmx|cod|com|csh|css|csv|dll|doc|docx|ear|eot|epub|exe|flac|flv|gif|gz|ico|ics|ief|jar|jfif|jpe|jpeg|jpg|less|m3u|mid|midi|mjs|mkv|mov|mp2|mp3|mp4|mpa|mpe|mpeg|mpg|mpkg|mpp|mpv2|odp|ods|odt|oga|ogg|ogv|ogx|otf|pbm|pdf|pgm|png|pnm|ppm|ppt|pptx|ra|ram|rar|ras|rgb|rmi|rtf|scss|sh|snd|svg|swf|tar|tif|tiff|ttf|vsd|war|wav|weba|webm|webp|wmv|woff|woff2|xbm|xls|xlsx|xpm|xul|xwd|zip";

    public static String host = "gh0st.cn";

    public static String status = "404";

    public static String header = "Last-Modified|Date|Connection|ETag";

    public static String size = "0";

    public static String boundary = "\n\t\n";

    public static String[] scope = new String[]{
            "any",
            "any header",
            "any body",
            "response",
            "response line",
            "response header",
            "response body",
            "request",
            "request line",
            "request header",
            "request body"
    };

    public static String scopeOptions = "Suite|Target|Proxy|Scanner|Intruder|Repeater|Logger|Sequencer|Decoder|Comparer|Extensions|Organizer|Recorded login replayer";

    public static String modeStatus = "true";

    public static String[] ruleFields = {
            "Loaded", "Name", "F-Regex", "S-Regex", "Format", "Color", "Scope", "Engine", "Sensitive"
    };

    public static Object[][] ruleTemplate = new Object[][]{
            {
                    false, "New Name", "(First Regex)", "(Second Regex)", "{0}", "gray", "any", "nfa", false
            }
    };

    public static String[] engine = new String[]{
            "nfa",
            "dfa"
    };

    public static String[] color = new String[]{
            "red",
            "orange",
            "yellow",
            "green",
            "cyan",
            "blue",
            "pink",
            "magenta",
            "gray"
    };

    public static Boolean proVersionStatus = true;

    public static Map<String, Object[][]> globalRules = new HashMap<>();

    public static ConcurrentHashMap<String, Map<String, List<String>>> globalDataMap = new ConcurrentHashMap<>();
}
