package com.cybzacg.blogbackend.module.article;

import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.enums.article.ArticleReviewStatusEnum;
import com.cybzacg.blogbackend.enums.article.ArticleVisibilityScopeEnum;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.service.ArticleStatusMachine;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ArticleStatusMachineTest {
    private final ArticleStatusMachine machine = new ArticleStatusMachine();

    @Test
    void normalizeStatusShouldRejectIllegalValue() {
        assertThrows(BusinessException.class, () -> machine.normalizeStatus(3));
    }

    @Test
    void normalizeReviewStatusShouldDefaultToNotSubmitted() {
        assertEquals(ArticleReviewStatusEnum.NOT_SUBMITTED.getValue(), machine.normalizeReviewStatus(null));
    }

    @Test
    void normalizeVisibilityScopeShouldDefaultToPublic() {
        assertEquals(ArticleVisibilityScopeEnum.PUBLIC.getValue(), machine.normalizeVisibilityScope(null));
    }

    @Test
    void validateSaveStateShouldRejectFutureScheduleOnRejectedArticle() {
        assertThrows(BusinessException.class, () -> machine.validateSaveState(
                1, ArticleReviewStatusEnum.REJECTED.getValue(), 0, 0, LocalDateTime.now().plusDays(1)));
    }

    @Test
    void canShowInPublicListShouldRespectStatusAndVisibility() {
        BlogArticle article = new BlogArticle();
        article.setStatus(1);
        article.setReviewStatus(ArticleReviewStatusEnum.APPROVED.getValue());
        article.setVisibilityScope(ArticleVisibilityScopeEnum.PUBLIC.getValue());
        article.setAccessLevel(0);

        assertTrue(machine.canShowInPublicList(article));
    }
}
