package com.Dolmeng_E.workspace.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIProjectAnalysisResDto {

    /**
     * 📊 프로젝트 분석 리포트 (자연어 요약)
     * ex) "📊 프로젝트 분석 리포트\n진행률: 약 65.6% ..."
     */
    private String analysisReport;

    /**
     * 📈 예상 완료일 신뢰도 추세 데이터 (그래프용)
     * ex) [{"date":"2025-11-05","confidence":0.55}, ...]
     */
    private List<PredictedCompletionTrend> predictedCompletionTrend;

    /**
     * ⚠️ 리스크 요인 분석 데이터
     * ex) [{"factor":"지연 태스크 증가","riskLevel":0.7}, ...]
     */
    private List<RiskFactor> riskFactors;

    // --- 내부 클래스 정의 ---

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PredictedCompletionTrend {
        /**
         * 예측 날짜 (ISO 8601 형식)
         * ex) "2025-11-05"
         */
        private String date;

        /**
         * 신뢰도 (0.0 ~ 1.0)
         */
        private double confidence;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiskFactor {
        /**
         * 리스크 요인 설명
         * ex) "지연 태스크 증가"
         */
        private String factor;

        /**
         * 위험도 수준 (0.0 ~ 1.0)
         */
        private double riskLevel;
    }
}
