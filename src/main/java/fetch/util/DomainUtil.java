package fetch.util;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author macia
 * @date 2023/4/5
 */
@Slf4j
public class DomainUtil {

    public static String getMostPattern(String hostUrl,List<String> urls){
        Set<String> nestResult = new HashSet<>(urls);
        Set<String> mostResult = getMostUrl(nestResult);
        return getPattern(mostResult);
    }

    private static String getPattern(Set<String> mostResult) {
        List<SeedSegment> urlSegments = new ArrayList<>();
        int segMaxLen = 0;
        for (String seed : mostResult) {
            if (StringUtils.isNotEmpty(seed)) {
                String[] segments = seed.split("/");
                segMaxLen = Math.max(segments.length, segMaxLen);
                for (int i = 0; i < segments.length; i++) {
                    if (urlSegments.size() <= i) {
                        urlSegments.add(new SeedSegment());
                    }
                    String segment = segments[i];
                    SeedSegment pointSegment = urlSegments.get(i);
                    segmentInfoExact(segment, pointSegment);
                }
            }
        }
        return generateBySeedSegment(urlSegments);
    }
    private static String generateBySeedSegment(List<SeedSegment> segment) {
        System.out.printf("segment info :\n%s\n", JSONObject.toJSONString(segment, true));
        StringBuilder pattern = new StringBuilder();
        boolean hasTime = false;
        int timeStarIndex = 0, timeEndIndex = 0;
        StringBuilder timeBuilder = new StringBuilder();
        for (int i = 0; i < segment.size(); i++) {
            SeedSegment seedSegment = segment.get(i);
            if (i + 1 < segment.size()) {
                int i1 = timeContextSniff(seedSegment, timeBuilder, segment, i);
                if (i1 != -1) {
                    if (!hasTime) {
                        timeStarIndex = i;
                        timeEndIndex = i1;
                        hasTime = true;
                    }
                    pattern.append(timeBuilder);
                    i = i1;
                    continue;
                }
            }
            // 最后一个处理
//            if (i == segment.size() - 1) {
                // 后缀特殊区分
//                StringBuilder suffixBuilderStr = new StringBuilder();
                // 细化文章尾部正则匹配模式
//                suffixLabelSniff(seedSegment, suffixBuilderStr);
//                return pattern.append(suffixBuilderStr).toString();
//                return "http.*?" + timeBuilder.append(suffixBuilderStr).toString();
                // 两种形式均存在
//                // 时间与尾部无间隔
//                if ((timeEndIndex + 1) == i) {
//                    log.info("含有时间且与尾部标识无间隔,准确率较高");
//                    if (timeStarIndex == 0) {
//                        return "/" + timeBuilder.append(suffixBuilderStr);
//                    } else {
//                        return "/.*?/" + timeBuilder.append(suffixBuilderStr);
//                    }
//                } else {
//                    log.info("含有时间且与尾部标识存在间隔,需要人工审核.");
//                    //存在间隔
//                    for (; timeEndIndex < i; timeEndIndex++) {
//                        urlGenerateBySegment(timeBuilder, segment.get(i));
//                    }
//                    if (timeStarIndex == 0) {
//                        return "/" + timeBuilder.append(suffixBuilderStr);
//                    } else {
//                        return "/.*?/" + timeBuilder.append(suffixBuilderStr);
//                    }
//                }
//            }
            // 通过segment生成匹配模式
            urlGenerateBySegment(pattern, seedSegment);
        }
        pattern.deleteCharAt(pattern.length() - 1).append("$");
        return pattern.toString();
    }

    /**
     * 后缀识别处理 html shtml htm
     *
     * @param seedSegment
     * @param pattern
     * @return
     */
    private static void suffixLabelSniff(SeedSegment seedSegment, StringBuilder pattern) {
        List<String> segmentList = seedSegment.getSegmentList();
        int x = seedSegment.getIsSameLen() ? seedSegment.getSegmentLen() : seedSegment.getMaxLen();
        int y = segmentList.size();
        for (int i = 0; i < x; i++) {
            boolean isLost = false;
            boolean isSame = true;
            boolean isDigest = true;
            boolean isLetter = true;
            boolean isSymbol = true;
            Character character = null;
            for (int j = 0; j < y; j++) {
                String yn = segmentList.get(j);
                if (yn.length() > i) {
                    // 长度有可能相同
                    char xn = yn.charAt(i);
                    if (character == null) {
                        character = xn;
                    } else {
                        isSame = isSame && xn == character;
                        isDigest = isDigest && Character.isDigit(xn);
                        isLetter = isLetter && Character.isLetter(xn);
                        isSymbol = isSymbol && !Character.isDigit(xn) && !Character.isLetter(xn);
                    }
                } else {
                    // 长度不相同
                    isLost = true;
                }
            }
            // 结果输出
            output(isLost, isSame, isDigest, isLetter, isSymbol, character, pattern);
        }
        pattern.append("$");
    }

    private static void output(boolean isLost, boolean isSame, boolean isDigest, boolean isLetter, boolean isSymbol, Character character, StringBuilder outputBuilder) {
        if (isLost) {
            outputBuilder.append(".?");
            return;
        }
        if (isSame) {
            if (isDigest) {
                outputBuilder.append("\\\\d");
            } else if (isLetter) {
                outputBuilder.append(".");
            } else if (isSymbol) {
                outputBuilder.append(character);
            }
        } else {
            if (isDigest) {
                outputBuilder.append("\\\\d");
            } else if (isLetter) {
                outputBuilder.append(".");
            } else if (isSymbol) {
                outputBuilder.append(".");
            }
        }
    }

    private static void urlGenerateBySegment(StringBuilder pattern, SeedSegment seedSegment) {
        String lenPattern = seedSegment.getIsSameLen() ? "{" + seedSegment.getMinLen() + "}" : "{" + seedSegment.getMinLen() + "," + seedSegment.getMaxLen() + "}";
        String strPattern = seedSegment.getIsEquals() ? seedSegment.getSegmentStr() : seedSegment.getIsSpecialSymbol() ? "." : seedSegment.getIsDigit() && !seedSegment.getIsLetter() ? "\\\\d" : ".";
        pattern.append(strPattern);
        if (!seedSegment.getIsEquals()) {
            pattern.append(lenPattern);
        }
        pattern.append("/");
    }

    private static void segmentInfoExact(String segment, SeedSegment pointSegment) {
        String segmentStr = pointSegment.getSegmentStr();
        if (StringUtils.isBlank(segmentStr)) {
            pointSegment.setSegmentStr(segment);
            pointSegment.setMaxLen(segment.length());
            pointSegment.setMinLen(segment.length());
        }
        if (pointSegment.getIsEquals()) {
            pointSegment.setIsEquals(pointSegment.getSegmentStr().equals(segment));
        }
        // 第一次对比或者相等逻辑
        if (pointSegment.getIsSameLen()) {
            int pointSegmentLen = pointSegment.getSegmentStr().length();
            pointSegment.setIsSameLen(pointSegmentLen == segment.length());
            if (pointSegment.getSegmentLen() == null) {
                pointSegment.setSegmentLen(pointSegmentLen);
            }
        }
        pointSegment.setMaxLen(Math.max(pointSegment.getMaxLen(), segment.length()));
        pointSegment.setMinLen(Math.min(pointSegment.getMinLen(), segment.length()));
        if (StringUtils.isNotEmpty(segment)) {
            for (int j = 0; j < segment.length(); j++) {
                char c = segment.charAt(j);
                boolean letter = Character.isLetter(c);
                boolean digit = Character.isDigit(c);
                if (pointSegment.getIsDigit() == null) {
                    pointSegment.setIsDigit(digit);
                }
                if (pointSegment.getIsLetter() == null) {
                    pointSegment.setIsLetter(letter);
                }
                if (digit) {
                    pointSegment.setIsDigit(true);
                }
                if (letter) {
                    pointSegment.setIsLetter(true);
                }
                if (!digit && !letter) {
                    pointSegment.setIsSpecialSymbol(true);
                    break;
                }
                // 混合
                if (pointSegment.getIsDigit() && pointSegment.getIsLetter()) {
                    pointSegment.setIsSpecialSymbol(true);
                    break;
                }
            }
        } else {
            pointSegment.setIsDigit(false);
            pointSegment.setIsLetter(false);
            pointSegment.setIsSpecialSymbol(false);
        }
        if (pointSegment.getIsSpecialSymbol() == null) {
            pointSegment.setIsSpecialSymbol(false);
        }
        List<String> list = pointSegment.getSegmentList() == null ? new ArrayList<>() : pointSegment.getSegmentList();
        list.add(segment);
        pointSegment.setSegmentList(list);
    }

    private static Set<String> getMostUrl(Set<String> neatResult) {
        Map<String, Set<String>> hrefLenMap = new HashMap<>(24);
        for (String domainHref : neatResult) {
            int hrefLen = domainHref.length();
            String key = String.valueOf(hrefLen);
            Set<String> domainSet = hrefLenMap.get(key);
            if (domainSet == null) {
                Set<String> newSet = new HashSet<>();
                newSet.add(domainHref);
                hrefLenMap.put(key, newSet);
            } else {
                domainSet.add(domainHref);
            }
        }
        Map<String,String> top2Info = getTop2(hrefLenMap);
        Set<String> top1HrefSet = hrefLenMap.get(String.valueOf(top2Info.get("top1")));
        int top1Len = Integer.parseInt(top2Info.get("top1Len"));
        int top2Len = Integer.parseInt(top2Info.get("top2Len"));
         // 2. 长度对比
        if (top2Len != 0) {
            if (top1Len - top2Len < 5) {
                log.info("长度相差小于5");
                //长度相差不大,数量差对比
                Set<String> top2HrefSet = hrefLenMap.get(String.valueOf(top2Len));
                int top1Size = top1HrefSet.size();
                int top2Size = top2HrefSet.size();
                if (top1Size - top2Size < 20) {
                    log.info("数差大于20,跳过多层匹配");
                    top1HrefSet.addAll(top2HrefSet);
                }
            }
        }
        return top1HrefSet;
    }
    private static Map<String, String> getTop2(Map<String, Set<String>> hrefLenMap) {
        int top1Size = 0;
        int top2Size = 0;
        String top1Key = null;
        String top2Key = null;
        int top1Len = 0;
        int top2Len = 0;
        for (Map.Entry<String, Set<String>> entry : hrefLenMap.entrySet()) {
            String key = entry.getKey();
            int valSize = entry.getValue().size();
            int len = entry.getValue().iterator().next().length();;
            if (valSize >= top1Size) {
                top1Size = valSize;
                top1Key = key;
                top1Len = len;
            } else {
                top2Size = Math.max(valSize, top2Size);
                top2Key = key;
                top2Len = len;
            }
        }
        Map<String,String> result = new HashMap<>(4);
        result.put("top1",top1Key);
        result.put("top1Len",String.valueOf(top1Len));
        result.put("top2",top2Key);
        result.put("top2Len",String.valueOf(top2Len));
        return result;
    }
    private static Set<String> denoising(String hostUrl, List<String> urls) {
        String host = URI.create(hostUrl).getHost();
        Set<String> result = new HashSet();
        for (String url : urls) {
            int index = url.indexOf("#");
            if (index != -1) {
                url = url.substring(0, index);
            }
            URI uri = null;
            try {
                uri = URI.create(url);
                if (host.equals(uri.getHost())) {
                    String path = uri.getPath();
                    while (path.length() > 0 && '/' == path.charAt(0)) {
                        path = path.substring(1);
                    }
                    result.add(path);
                }
            } catch (IllegalArgumentException argumentException) {
                log.warn("异常链接：{}", url);
            }
        }
        return result;
    }


    // 年月
    private static final Pattern TIME_FIXED_LEN_YEAR_MONTH_WITH_STR_PATTERN = Pattern.compile("20[012]\\d.[01]\\d");
    private static final Pattern TIME_UNFIXED_LEN_YEAR_MONTH_WITH_STR_PATTERN = Pattern.compile("20[012]\\d.[01]?\\d");
    private static final Pattern TIME_YEAR_MONTH_NO_STR_PATTERN = Pattern.compile("20[012]\\d[01]\\d");

    // 月
    private static final Pattern TIME_FIXED_LEN_MONTH_PATTERN = Pattern.compile("[01]\\d");
    private static final Pattern TIME_UNFIXED_LEN_MONTH_PATTERN = Pattern.compile("[01]?\\d");

    // 日
    private static final Pattern TIME_FIXED_LEN_DAY_PATTERN = Pattern.compile("[0123]\\d");
    private static final Pattern TIME_UNFIXED_LEN_DAY_PATTERN = Pattern.compile("[0123]?\\d");
    // 年
    private static final Pattern TIME_YEAR_PATTERN = Pattern.compile("20[012]\\d");

    // 月日
    private static final Pattern TIME_FIXED_MONTH_DAY_PATTERN = Pattern.compile("[01]\\d[0123]\\d");
    private static final Pattern TIME_UNFIXED_MONTH_DAY_PATTERN = Pattern.compile("[01]?\\d[0123]?\\d");
    private static final Pattern TIME_MONTH_DAY_PATTERN = Pattern.compile("[01]?\\d.[0123]?\\d");

    // 年月日
    private static final Pattern TIME_YEAR_MONTH_DAY_WITH_STR_PATTERN = Pattern.compile("20[012]\\d.?[01]?\\d.?[0123]?\\d");
    private static final Pattern TIME_YEAR_MONTH_DAY_WITH_NO_PATTERN = Pattern.compile("20[012]\\d[01]?\\d[0123]?\\d");

    /**
     * @param currentSeedSegment
     * @param outputBuilderStr
     * @param segment
     * @param currentPoint       细化时间正则
     * @return -1 继续执行，其他 continue
     */
    private static int timeContextSniff(SeedSegment currentSeedSegment, StringBuilder outputBuilderStr, List<SeedSegment> segment, int currentPoint) {
        List<String> segmentList = currentSeedSegment.getSegmentList();
        // 带特殊字符日期处理 /2021-08/05
        if (currentSeedSegment.getIsSameLen() && currentSeedSegment.getSegmentLen() == 7 && currentSeedSegment.getIsSpecialSymbol() && segmentList.stream().allMatch(it -> TIME_FIXED_LEN_YEAR_MONTH_WITH_STR_PATTERN.matcher(it).find())) {
            outputBuilderStr.append("20[012]\\\\d.[01]\\\\d/");
            // 后缀日份探测
            SeedSegment nextSeedSegment = segment.get(currentPoint + 1);
            // 日份固定长度为2 /2021-08/05
            if (!nextSeedSegment.getIsSpecialSymbol() && nextSeedSegment.getIsDigit() && nextSeedSegment.getIsSameLen() && nextSeedSegment.getSegmentLen() == 2 && nextSeedSegment.getSegmentList().stream().allMatch(it -> TIME_FIXED_LEN_DAY_PATTERN.matcher(it).find())) {
                outputBuilderStr.append("[0123]\\\\d/");
                return currentPoint + 1;
            }
            // 日份非固定长度匹配 /2021-08/5
            if (!nextSeedSegment.getIsSpecialSymbol() && nextSeedSegment.getIsDigit() && nextSeedSegment.getMinLen() == 1 && nextSeedSegment.getMaxLen() == 2 && nextSeedSegment.getSegmentList().stream().allMatch(it -> TIME_UNFIXED_LEN_DAY_PATTERN.matcher(it).find())) {
                outputBuilderStr.append("[0123]?\\\\d/");
                return currentPoint + 1;
            }
            return currentPoint;
        }
        ///2021-8/05
        if (currentSeedSegment.getMaxLen() == 7 && currentSeedSegment.getMinLen() == 6 && currentSeedSegment.getIsSpecialSymbol() && segmentList.stream().allMatch(it -> TIME_UNFIXED_LEN_YEAR_MONTH_WITH_STR_PATTERN.matcher(it).find())) {
            outputBuilderStr.append("20[012]\\\\d.[01]?\\\\d/");
            // 后缀月份探测
            SeedSegment nextSeedSegment = segment.get(currentPoint + 1);
            // 月份固定长度为2 /2021-8/05
            if (!nextSeedSegment.getIsSpecialSymbol() && nextSeedSegment.getIsDigit() && nextSeedSegment.getIsSameLen() && nextSeedSegment.getSegmentLen() == 2 && nextSeedSegment.getSegmentList().stream().allMatch(it -> TIME_FIXED_LEN_DAY_PATTERN.matcher(it).find())) {
                outputBuilderStr.append("[0123]\\\\d/");
                return currentPoint + 1;
            }
            // 非固定长度匹配 /2021-8/5
            if (!nextSeedSegment.getIsSpecialSymbol() && nextSeedSegment.getIsDigit() && nextSeedSegment.getMinLen() == 1 && nextSeedSegment.getMaxLen() == 2 && nextSeedSegment.getSegmentList().stream().allMatch(it -> TIME_UNFIXED_LEN_DAY_PATTERN.matcher(it).find())) {
                outputBuilderStr.append("0123]?\\\\d/");
                return currentPoint + 1;
            }
            return currentPoint;
        }

        // /xwzx/la/202302/t20230221_12512191.htm
        // /article/202303/20230303105723828.html
        if (currentSeedSegment.getIsSameLen() && currentSeedSegment.getSegmentLen() == 6 && !currentSeedSegment.getIsSpecialSymbol() && currentSeedSegment.getIsDigit() && segmentList.stream().anyMatch(it -> TIME_YEAR_MONTH_NO_STR_PATTERN.matcher(it).find())) {
            outputBuilderStr.append("20[012]\\\\d[01]\\\\d/");
            // 探测后缀 日份
            SeedSegment nextSeedSegment = segment.get(currentPoint + 1);
            // 日份固定长度为2 /202305/02
            if (!nextSeedSegment.getIsSpecialSymbol() && nextSeedSegment.getIsDigit() && nextSeedSegment.getIsSameLen() && nextSeedSegment.getSegmentLen() == 2 && nextSeedSegment.getSegmentList().stream().allMatch(it -> TIME_FIXED_LEN_DAY_PATTERN.matcher(it).find())) {
                outputBuilderStr.append("[0123]\\\\d/");
                return currentPoint + 1;
            } else
                // 日份非固定长度匹配 /202305/2
                if (!nextSeedSegment.getIsSpecialSymbol() && nextSeedSegment.getIsDigit() && nextSeedSegment.getMinLen() == 1 && nextSeedSegment.getMaxLen() == 2 && nextSeedSegment.getSegmentList().stream().allMatch(it -> TIME_UNFIXED_LEN_DAY_PATTERN.matcher(it).find())) {
                    outputBuilderStr.append("[0123]?\\\\d/");
                    return currentPoint + 1;
                }
            return currentPoint;
        }
        // 20231/01
        // 202301/01
        if (currentSeedSegment.getMinLen() == 5 && currentSeedSegment.getMaxLen() == 6 && !currentSeedSegment.getIsSpecialSymbol() && currentSeedSegment.getIsDigit() && segmentList.stream().anyMatch(it -> TIME_YEAR_MONTH_NO_STR_PATTERN.matcher(it).find())) {
            outputBuilderStr.append("20[012]\\\\d[01]?\\\\d/");
            // 探测后缀 日份
            SeedSegment nextSeedSegment = segment.get(currentPoint + 1);
            // 日份固定长度为2 /202305/02
            if (!nextSeedSegment.getIsSpecialSymbol() && nextSeedSegment.getIsDigit() && nextSeedSegment.getIsSameLen() && nextSeedSegment.getSegmentLen() == 2 && nextSeedSegment.getSegmentList().stream().allMatch(it -> TIME_FIXED_LEN_DAY_PATTERN.matcher(it).find())) {
                outputBuilderStr.append("[0123]\\\\d/");
                return currentPoint + 1;
            } else
                // 日份非固定长度匹配 /202305/2
                if (!nextSeedSegment.getIsSpecialSymbol() && nextSeedSegment.getIsDigit() && nextSeedSegment.getMinLen() == 1 && nextSeedSegment.getMaxLen() == 2 && nextSeedSegment.getSegmentList().stream().allMatch(it -> TIME_UNFIXED_LEN_DAY_PATTERN.matcher(it).find())) {
                    outputBuilderStr.append("[0123]?\\\\d/");
                    return currentPoint + 1;
                }
            return currentPoint;
        }
        // /2023/0213
        // /2021/05/06/
        if (currentSeedSegment.getIsSameLen() && currentSeedSegment.getSegmentLen() == 4 && !currentSeedSegment.getIsSpecialSymbol() && currentSeedSegment.getIsDigit() && segmentList.stream().anyMatch(it -> TIME_YEAR_PATTERN.matcher(it).find())) {
            outputBuilderStr.append("20[012]\\\\d/");
            SeedSegment nextSeedSegment = segment.get(currentPoint + 1);
            if (nextSeedSegment.getIsSpecialSymbol() && nextSeedSegment.getMinLen() > 3 && nextSeedSegment.getMaxLen() <=5 && nextSeedSegment.getSegmentList().stream().allMatch(it -> TIME_MONTH_DAY_PATTERN.matcher(it).find())){
                outputBuilderStr.append("[01]?\\\\d.[0123]?\\\\d/");
                return currentPoint + 1;
            }
            // 定长
            // 后长为4 2023/0213
            if (nextSeedSegment.getIsSameLen() && nextSeedSegment.getIsDigit() && nextSeedSegment.getSegmentLen() == 6 && nextSeedSegment.getSegmentList().stream().allMatch(it -> TIME_FIXED_MONTH_DAY_PATTERN.matcher(it).find())) {
                outputBuilderStr.append("[01]\\\\d[0123]\\\\d/");
                return currentPoint + 1;
            } else
                // 非定长
                // 后长为2 2021/56/
                // 后长为2 2021/1112/
                if (!nextSeedSegment.getIsSameLen() && nextSeedSegment.getIsDigit() && nextSeedSegment.getMaxLen() == 4 && nextSeedSegment.getMinLen() == 2 && nextSeedSegment.getSegmentList().stream().allMatch(it -> TIME_UNFIXED_MONTH_DAY_PATTERN.matcher(it).find())) {
                    outputBuilderStr.append("[01]?\\\\d[0123]?\\\\d/");
                    return currentPoint + 1;
                } else
                    // 2022/01/02 定长月份
                    if (nextSeedSegment.getIsSameLen() && nextSeedSegment.getIsDigit() && nextSeedSegment.getSegmentLen() == 2 && nextSeedSegment.getSegmentList().stream().allMatch(it -> TIME_FIXED_LEN_MONTH_PATTERN.matcher(it).find())) {
                        outputBuilderStr.append("[01]\\\\d/");
                        if (segment.size() > currentPoint + 2) {
                            SeedSegment daySegment = segment.get(currentPoint + 2);
                            // 定长 日份
                            if (daySegment.getIsDigit() && daySegment.getIsSameLen() && daySegment.getSegmentLen() == 2 && daySegment.getSegmentList().stream().allMatch(it -> TIME_FIXED_LEN_DAY_PATTERN.matcher(it).find())) {
                                outputBuilderStr.append("[0123]\\\\d/");
                                return currentPoint + 2;
                            }
                            // 非定长 日份
                            if (daySegment.getIsDigit() && !daySegment.getIsSameLen() && daySegment.getMaxLen() == 2 && daySegment.getMinLen() == 1 && daySegment.getSegmentList().stream().allMatch(it -> TIME_UNFIXED_LEN_DAY_PATTERN.matcher(it).find())) {
                                outputBuilderStr.append("[0123]?\\\\d/");
                                return currentPoint + 2;
                            }
                        }
                        return currentPoint + 1;
                    } else
                        // 2022/1/2 非定长月份
                        if (!nextSeedSegment.getIsSpecialSymbol() && nextSeedSegment.getIsDigit() && nextSeedSegment.getMinLen() == 1 && nextSeedSegment.getMinLen() == 2 && nextSeedSegment.getSegmentList().stream().allMatch(it -> TIME_UNFIXED_LEN_MONTH_PATTERN.matcher(it).find())) {
                            outputBuilderStr.append("20[012]\\\\d/[01]?\\d/");
                            if (segment.size() > currentPoint + 2) {
                                SeedSegment daySegment = segment.get(currentPoint + 2);
                                // 定长 日份
                                if (daySegment.getIsDigit() && daySegment.getIsSameLen() && daySegment.getSegmentLen() == 2 && daySegment.getSegmentList().stream().allMatch(it -> TIME_FIXED_LEN_DAY_PATTERN.matcher(it).find())) {
                                    outputBuilderStr.append("[0123]\\\\d/");
                                    return currentPoint + 2;
                                }
                                // 非定长 日份
                                if (daySegment.getIsDigit() && !daySegment.getIsSameLen() && daySegment.getMaxLen() == 2 && daySegment.getMinLen() == 1 && daySegment.getSegmentList().stream().allMatch(it -> TIME_UNFIXED_LEN_DAY_PATTERN.matcher(it).find())) {
                                    outputBuilderStr.append("[0123]?\\\\d/");
                                    return currentPoint + 2;
                                }
                            }
                            return currentPoint + 1;
                        }
            // 仅有年份
            return currentPoint;
        }
        // 年月日 带字符串 2022-01-01
        if (currentSeedSegment.getIsSpecialSymbol() && currentSeedSegment.getMaxLen() == 10 && currentSeedSegment.getMinLen() == 8 && currentSeedSegment.getSegmentList().stream().allMatch(it -> TIME_YEAR_MONTH_DAY_WITH_STR_PATTERN.matcher(it).find())) {
            outputBuilderStr.append("20[012]\\d.?[01]?\\d.?[0123]?\\d/");
            return currentPoint;
        }
        // 年月日 不带字符串 20220101
        if (!currentSeedSegment.getIsSpecialSymbol() && currentSeedSegment.isDigit && currentSeedSegment.getMaxLen() == 8 && currentSeedSegment.getMinLen() == 6 && currentSeedSegment.getSegmentList().stream().allMatch(it -> TIME_YEAR_MONTH_DAY_WITH_NO_PATTERN.matcher(it).find())) {
            outputBuilderStr.append("20[012]\\d[01]?\\d[0123]?\\d/");
            return currentPoint;
        }
        return -1;
    }

    @Data
    static
    class SeedSegment {
        String segmentStr;
        Boolean isDigit;
        Boolean isLetter;
        Boolean isSpecialSymbol;
        Boolean isEquals = true;
        Boolean isSameLen = true;
        Integer segmentLen;
        Integer maxLen;
        Integer minLen;
        List<String> segmentList;
    }
}
