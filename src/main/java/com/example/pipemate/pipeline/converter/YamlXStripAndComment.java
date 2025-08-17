package com.example.pipemate.pipeline.converter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YAML에서 x_* 로 시작하는 키와 그 하위 블록을 모두 주석 처리하는 유틸리티 클래스.
 * 예) x_name: ...  →  # x_name: ...
 */
public final class YamlXStripAndComment {

    // 선행 공백 + x_*: 형태를 매칭하는 정규식
    private static final Pattern X_KEY_PATTERN =
            Pattern.compile("^(\\s*)(x_[A-Za-z0-9_-]+):(.*)$");

    private YamlXStripAndComment() {}

    /**
     * 입력된 YAML 문자열에서 x_* 키와 그 하위 라인을 "# "로 주석 처리한다.
     */
    public static String transform(String yaml) {
        // 탭 문자는 공백 2칸으로 치환
        String norm = yaml.replace("\t", "  ");
        String[] lines = norm.split("\\r?\\n", -1);
        StringBuilder out = new StringBuilder(norm.length() + 256);

        int i = 0;
        while (i < lines.length) {
            String line = lines[i];

            // 이미 주석인 라인은 그대로 둔다
            if (line.trim().startsWith("#")) {
                out.append(line).append('\n');
                i++;
                continue;
            }

            Matcher m = X_KEY_PATTERN.matcher(line);
            if (!m.find()) {
                // x_* 키가 아니면 그대로 출력
                out.append(line).append('\n');
                i++;
                continue;
            }

            // 현재 x_* 키 라인 주석 처리
            int baseIndent = m.group(1).length();
            out.append("# ").append(line).append('\n');
            i++;

            // 하위 블록(더 들여쓰기된 라인들) 모두 주석 처리
            while (i < lines.length) {
                String next = lines[i];

                if (next.isEmpty()) {
                    // 빈 줄도 "# " 붙여서 주석 처리
                    out.append("# ").append(next).append('\n');
                    i++;
                    continue;
                }

                int nextIndent = countLeadingSpaces(next);

                if (nextIndent > baseIndent) {
                    // 이미 주석이면 그대로, 아니면 "# " 붙이기
                    if (next.trim().startsWith("#")) {
                        out.append(next).append('\n');
                    } else {
                        out.append("# ").append(next).append('\n');
                    }
                    i++;
                } else {
                    // 들여쓰기가 줄어들면 하위 블록 끝
                    break;
                }
            }
        }
        return out.toString();
    }

    // 문자열의 선행 공백 개수를 세는 함수
    private static int countLeadingSpaces(String s) {
        int c = 0;
        while (c < s.length() && s.charAt(c) == ' ') c++;
        return c;
    }
}