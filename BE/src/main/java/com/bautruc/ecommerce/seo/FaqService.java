package com.bautruc.ecommerce.seo;

import com.bautruc.ecommerce.common.exception.ResourceNotFoundException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FaqService {
    public record Item(Long id, String questionVi, String answerVi, String questionEn, String answerEn,
                       int position, boolean visible) {}
    public record Input(@NotBlank @Size(max = 500) String questionVi,
                        @NotBlank @Size(max = 5000) String answerVi,
                        @NotBlank @Size(max = 500) String questionEn,
                        @NotBlank @Size(max = 5000) String answerEn,
                        @Min(0) @Max(10000) int position, boolean visible) {}

    private final JdbcTemplate jdbc;
    public FaqService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public List<Item> publicItems() {
        return jdbc.query("SELECT id,question_vi,answer_vi,question_en,answer_en,position,visible FROM faq_items WHERE visible ORDER BY position,id", this::map);
    }

    @Transactional(readOnly = true)
    public List<Item> adminItems() {
        return jdbc.query("SELECT id,question_vi,answer_vi,question_en,answer_en,position,visible FROM faq_items ORDER BY position,id", this::map);
    }

    @Transactional
    public Item save(Long id, Input value) {
        if (id == null) id = jdbc.queryForObject("""
                INSERT INTO faq_items(question_vi,answer_vi,question_en,answer_en,position,visible)
                VALUES (?,?,?,?,?,?) RETURNING id
                """, Long.class, value.questionVi(), value.answerVi(), value.questionEn(), value.answerEn(),
                value.position(), value.visible());
        else if (jdbc.update("""
                UPDATE faq_items SET question_vi=?,answer_vi=?,question_en=?,answer_en=?,position=?,visible=?,updated_at=now()
                WHERE id=?
                """, value.questionVi(), value.answerVi(), value.questionEn(), value.answerEn(), value.position(), value.visible(), id) == 0)
            throw new ResourceNotFoundException("FAQ_NOT_FOUND", "FAQ item not found.");
        long savedId = id;
        return adminItems().stream().filter(item -> item.id() == savedId).findFirst().orElseThrow();
    }

    @Transactional
    public void delete(Long id) {
        if (jdbc.update("DELETE FROM faq_items WHERE id=?", id) == 0)
            throw new ResourceNotFoundException("FAQ_NOT_FOUND", "FAQ item not found.");
    }

    private Item map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new Item(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getInt(6), rs.getBoolean(7));
    }
}
