package db.migration;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V30__seed_blog_drafts extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        for (String slug : new String[]{"lang-gom-bau-truc-o-dau", "mua-gom-bau-truc-lam-qua-tet"}) {
            try (var input = getClass().getResourceAsStream("/seo/blog/" + slug + ".md")) {
                if (input == null) throw new IllegalStateException("Missing approved blog draft " + slug);
                String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                String[] lines = content.split("\\R", 6);
                String title = lines[0].substring("Title: ".length());
                String description = lines[1].substring("Meta: ".length());
                try (PreparedStatement insert = context.getConnection().prepareStatement("""
                        INSERT INTO blog_posts(slug,title,body_markdown,seo_title,seo_description,status)
                        VALUES (?,?,?,?,?,'DRAFT') ON CONFLICT (slug) DO NOTHING
                        """)) {
                    insert.setString(1, slug);
                    insert.setString(2, content.substring(content.indexOf("# ") + 2).split("\\R", 2)[0]);
                    insert.setString(3, content);
                    insert.setString(4, title);
                    insert.setString(5, description);
                    insert.executeUpdate();
                }
            }
        }
    }
}
