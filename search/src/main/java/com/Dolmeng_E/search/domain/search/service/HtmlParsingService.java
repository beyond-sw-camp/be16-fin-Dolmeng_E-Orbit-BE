package com.Dolmeng_E.search.domain.search.service;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

@Service
public class HtmlParsingService {

    /**
     * HTML 문자열에서 모든 태그를 제거하고 순수한 텍스트만 반환합니다.
     *
     * @param html 입력받은 HTML 문자열
     * @return 태그가 제거된 순수 텍스트
     */
    public String extractText(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        // Jsoup.parse(html).text() 메서드가
        // HTML을 파싱하고 모든 태그를 제거한 뒤 텍스트만 반환합니다.
        return Jsoup.parse(html).text();
    }
}
