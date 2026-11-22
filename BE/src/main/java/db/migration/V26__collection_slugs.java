package db.migration;

import com.bautruc.ecommerce.catalog.domain.CollectionSlug;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V26__collection_slugs extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE collections ADD COLUMN slug varchar(300)");
            statement.execute("ALTER TABLE collections ADD COLUMN seo_title varchar(180)");
            statement.execute("ALTER TABLE collections ADD COLUMN seo_description varchar(500)");
        }
        var used = new java.util.HashSet<String>();
        try (PreparedStatement collections = connection.prepareStatement("SELECT id, name_vi FROM collections ORDER BY id");
             PreparedStatement update = connection.prepareStatement("UPDATE collections SET slug = ? WHERE id = ?");
             ResultSet rows = collections.executeQuery()) {
            while (rows.next()) {
                String slug = CollectionSlug.fromName(rows.getString("name_vi"), rows.getLong("id"));
                if (!used.add(slug)) slug += "-" + rows.getLong("id");
                update.setString(1, slug);
                update.setLong(2, rows.getLong("id"));
                update.addBatch();
            }
            update.executeBatch();
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE collections ALTER COLUMN slug SET NOT NULL");
            statement.execute("CREATE UNIQUE INDEX uk_collections_slug ON collections(slug)");
        }
    }
}
