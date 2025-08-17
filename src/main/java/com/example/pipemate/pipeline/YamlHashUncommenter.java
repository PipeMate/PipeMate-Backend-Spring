package com.example.pipemate.pipeline;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YamlHashUncommenter {

    // 선행 공백들 + '#' + (선택) 공백 1칸 + 나머지
    private static final Pattern LEADING_HASH_ONCE =
            Pattern.compile("^(\\s*)#( ?)(.*)$");

    private YamlHashUncommenter() {}

    /**
     * 각 라인에서 "선행 공백들 + # + (있다면) 공백 1개"만 제거합니다.
     * - 들여쓰기는 보존됩니다.
     * - # 뒤 공백은 '한 칸만' 제거합니다(두 칸 이상 있어도 한 칸만 제거).
     * - 라인 중간의 # 는 건드리지 않습니다(맨 앞 공백 다음 첫 # 만 처리).
     */
    public static String transform(String yaml) {
        // 탭이 있다면 들여쓰기 오차 방지 위해 스페이스로(선택)
        String norm = yaml.replace("\t", "  ");
        String[] lines = norm.split("\\r?\\n", -1);

        StringBuilder out = new StringBuilder(norm.length() + 256);
        for (String line : lines) {
            Matcher m = LEADING_HASH_ONCE.matcher(line);
            if (m.find()) {
                // group(1): 원래 선행 공백, group(2): '# 후 공백(있으면 1칸)', group(3): 나머지
                // => '#' 과 그 뒤 공백 1칸만 제거
                out.append(m.group(1))  // 원래 선행 공백 유지
                        .append(m.group(3))  // 내용
                        .append('\n');
            } else {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }
}