package db.migration;

import com.bautruc.ecommerce.catalog.domain.ProductSlug;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V24__product_slugs extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE products ADD COLUMN slug varchar(300)");
        }
        try (PreparedStatement products = connection.prepareStatement("SELECT id, name_vi FROM products");
             PreparedStatement update = connection.prepareStatement("UPDATE products SET slug = ? WHERE id = ?");
             ResultSet rows = products.executeQuery()) {
            while (rows.next()) {
                update.setString(1, ProductSlug.fromName(rows.getString("name_vi"), rows.getLong("id")));
                update.setLong(2, rows.getLong("id"));
                update.addBatch();
            }
            update.executeBatch();
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE products ALTER COLUMN slug SET NOT NULL");
            statement.execute("CREATE UNIQUE INDEX uk_products_slug ON products(slug)");
        }
    }
}
