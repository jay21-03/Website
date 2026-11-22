package com.bautruc.ecommerce.seo;

import com.bautruc.ecommerce.catalog.application.CatalogService;
import com.bautruc.ecommerce.catalog.application.ProductImageService;
import com.bautruc.ecommerce.catalog.domain.Product;
import com.bautruc.ecommerce.catalog.domain.ProductCollection;
import com.bautruc.ecommerce.common.config.ApplicationProperties;
import com.bautruc.ecommerce.sitecontent.application.HomepageContentService;
import com.bautruc.ecommerce.sitecontent.application.SitePresentationService;
import com.bautruc.ecommerce.sitecontent.application.AboutContentService;
import com.bautruc.ecommerce.support.application.SupportSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
public class SeoPageController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SeoPageController.class);
    private static final String SITE = "https://dangxembautruc.com";
    private static final MediaType HTML = new MediaType(MediaType.TEXT_HTML, java.nio.charset.StandardCharsets.UTF_8);
    private static final Map<String, String> PAGE_NAMES = Map.of(
            "/san-pham", "Sản phẩm gốm Bàu Trúc",
            "/trai-nghiem", "Trải nghiệm làm gốm Bàu Trúc",
            "/ve-chung-toi", "Về xưởng gốm Đàng Xem",
            "/lien-he", "Liên hệ xưởng gốm Đàng Xem",
            "/faq", "Câu hỏi thường gặp",
            "/chinh-sach", "Chính sách mua hàng");

    private final CatalogService catalog;
    private final ProductImageService images;
    private final HomepageContentService homepage;
    private final SitePresentationService presentation;
    private final AboutContentService about;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final SeoMetadataService metadata;
    private final SupportSettingsService support;
    private final ObjectMapper json;
    private final BlogService blog;
    private final FaqService faq;
    private final URI shellUri;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private volatile String shell;
    private volatile Instant shellExpires = Instant.EPOCH;

    public SeoPageController(CatalogService catalog, ProductImageService images, HomepageContentService homepage,
                             SitePresentationService presentation, ApplicationProperties properties,
                             org.springframework.jdbc.core.JdbcTemplate jdbc, SeoMetadataService metadata,
                             SupportSettingsService support, ObjectMapper json, AboutContentService about, BlogService blog,
                             FaqService faq) {
        this.catalog = catalog;
        this.images = images;
        this.homepage = homepage;
        this.presentation = presentation;
        this.about = about;
        this.jdbc = jdbc;
        this.metadata = metadata;
        this.support = support;
        this.json = json;
        this.blog = blog;
        this.faq = faq;
        this.shellUri = URI.create(properties.frontendBaseUrl().replaceAll("/$", "") + "/index.html");
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> home() {
        String heading = homepage.current().slogan().vi();
        var business = support.current();
        return page("/", heading, "Gốm Bàu Trúc thủ công từ xưởng Đàng Xem.", homepage.current().media().stream()
                .filter(item -> item.slot().name().equals("HOME_HERO")).map(item -> item.url()).findFirst().orElse(null), heading,
                Map.of("@context", "https://schema.org", "@type", "Store", "name", "Đàng Xem",
                        "url", SITE, "telephone", business.getZaloPhone(), "email", business.getEmail(),
                        "address", business.getAddress()));
    }

    @GetMapping(value = {"/san-pham", "/trai-nghiem", "/ve-chung-toi", "/lien-he", "/faq", "/chinh-sach"},
            produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> staticPage(jakarta.servlet.http.HttpServletRequest request) {
        String path = request.getRequestURI();
        String name = PAGE_NAMES.get(path);
        if (name == null) return notFound();
        String key = switch (path) {
            case "/san-pham" -> "catalog";
            case "/trai-nghiem" -> "workshop";
            case "/ve-chung-toi" -> "about";
            case "/lien-he" -> "support";
            case "/faq" -> "faq";
            default -> "policy";
        };
        var content = presentation.current().content();
        if (content != null && content.pages() != null) {
            var value = content.pages().getOrDefault(key, Map.of()).get("title");
            if (value != null && !value.vi().isBlank()) name = value.vi();
        }
        return page(path, name, name + " tại xưởng gốm Đàng Xem.", null, name,
                path.equals("/faq") ? faqSchema() : null);
    }

    @GetMapping(value = "/products/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> legacyProduct(@PathVariable String id) {
        if (!id.matches("[0-9]+")) return notFound();
        try {
            return redirect("/san-pham/" + catalog.requirePublicProduct(id).getSlug());
        } catch (com.bautruc.ecommerce.common.exception.ResourceNotFoundException ex) {
            return notFound();
        }
    }

    @GetMapping(value = "/san-pham/{slug}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> productOrCollection(@PathVariable String slug) {
        Optional<ProductCollection> group = catalog.publicCollection(slug);
        if (group.isPresent()) {
            ProductCollection c = group.get();
            var saved = metadata.forPath("/san-pham/" + slug);
            String title = nonblank(c.getSeoTitle(), nonblank(saved.title(), c.getNameVi() + " | Gốm Bàu Trúc Đàng Xem"));
            return page("/san-pham/" + slug, title, nonblank(c.getSeoDescription(), nonblank(saved.description(), c.getDescriptionVi())), null, c.getNameVi());
        }
        try {
            Product p = catalog.requirePublicProduct(slug);
            String title = nonblank(p.getSeoTitle(), p.getNameVi() + " | Gốm Bàu Trúc Đàng Xem");
            String description = nonblank(p.getSeoDescription(), summary(p.getDescriptionVi()));
            String image = images.thumbnailUrl(p.getId());
            long available = jdbc.query("SELECT quantity - reserved_quantity FROM inventories WHERE product_id = ?",
                    (rs, row) -> rs.getLong(1), p.getId()).stream().findFirst().orElse(0L);
            var offer = Map.of("@type", "Offer", "priceCurrency", "VND", "price", catalog.sellingPrice(p),
                    "availability", "https://schema.org/" + (available > 0 ? "InStock" : "OutOfStock"),
                    "url", SITE + "/san-pham/" + slug);
            var schema = new java.util.LinkedHashMap<String, Object>();
            schema.put("@context", "https://schema.org");
            schema.put("@type", "Product");
            schema.put("name", p.getNameVi());
            schema.put("description", description);
            if (image != null) schema.put("image", image);
            schema.put("offers", offer);
            return page("/san-pham/" + slug, title, description, image, p.getNameVi(), schema);
        } catch (com.bautruc.ecommerce.common.exception.ResourceNotFoundException ignored) {
            var destinations = jdbc.query("SELECT p.slug FROM product_slug_redirects r JOIN products p ON p.id = r.product_id WHERE r.old_slug = ? AND p.status = 'ACTIVE' AND p.deleted_at IS NULL",
                    (rs, row) -> rs.getString(1), slug);
            return destinations.isEmpty() ? notFound() : redirect("/san-pham/" + destinations.get(0));
        }
    }

    @GetMapping(value = "/blog", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> blogIndex() {
        var content = presentation.current().content();
        var title = content == null || content.pages() == null
            ? null
            : content.pages().getOrDefault("blog", Map.of()).get("heroTitle");

        String heading = title == null
            ? "Câu chuyện gốm Chăm Bàu Trúc"
            : title.vi();

        return page(
            "/blog",
            heading,
            "Bài viết về làng gốm và sản phẩm Bàu Trúc.",
            null,
            heading
        );
    }

    @GetMapping(value = "/blog/{slug}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> blogPost(@PathVariable String slug) {
        try {
            var post = blog.publicPost(slug);
            return page("/blog/" + slug, nonblank(post.seoTitle(), post.title()),
                    nonblank(post.seoDescription(), summary(post.bodyMarkdown())),
                    post.imageUrl(), post.title());
        } catch (com.bautruc.ecommerce.common.exception.ResourceNotFoundException ex) {
            return notFound();
        }
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        var urls = new java.util.LinkedHashSet<String>();
        urls.add("/");
        urls.addAll(PAGE_NAMES.keySet());
        jdbc.query("SELECT slug FROM collections WHERE status = 'ACTIVE' AND deleted_at IS NULL ORDER BY id",
                (rs, row) -> rs.getString(1)).forEach(slug -> urls.add("/san-pham/" + slug));
        jdbc.query("SELECT slug FROM products WHERE status = 'ACTIVE' AND deleted_at IS NULL ORDER BY id",
                (rs, row) -> rs.getString(1)).forEach(slug -> urls.add("/san-pham/" + slug));
        if (!blog.publicPosts().isEmpty()) urls.add("/blog");
        blog.publicPosts().forEach(post -> urls.add("/blog/" + post.slug()));
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        urls.forEach(path -> xml.append("<url><loc>").append(SITE).append(path).append("</loc></url>"));
        return ResponseEntity.ok().cacheControl(CacheControl.noCache())
                .contentType(MediaType.APPLICATION_XML).body(xml.append("</urlset>").toString());
    }

    private ResponseEntity<String> page(String path, String title, String description, String image) {
        return page(path, title, description, image, title);
    }

    private ResponseEntity<String> page(String path, String title, String description, String image, String heading) {
        return page(path, title, description, image, heading, null);
    }

    private ResponseEntity<String> page(String path, String title, String description, String image, String heading,
                                        Object schema) {
        try {
            var configured = metadata.forPath(path);
            var defaults = metadata.forPath("default");
            boolean dynamic = path.startsWith("/san-pham/") || path.startsWith("/blog/");
            String cleanTitle = escape(dynamic ? title : nonblank(configured.title(), nonblank(defaults.title(), title)));
            String cleanDescription = escape(dynamic ? nonblank(description, "Gốm Bàu Trúc thủ công tại Đàng Xem.") : nonblank(configured.description(), nonblank(defaults.description(),
                    nonblank(description, "Gốm Bàu Trúc thủ công tại Đàng Xem."))));
            String ogTitle = escape(nonblank(configured.ogTitle(), dynamic ? title :
                    nonblank(defaults.ogTitle(), configured.title() == null ? title : configured.title())));
            String ogDescription = escape(nonblank(configured.ogDescription(), dynamic ? description :
                    nonblank(defaults.ogDescription(), configured.description() == null ? description : configured.description())));
            String ogImage = nonblank(image, metadata.imageUrl(nonblank(configured.imageSlot(), defaults.imageSlot())));
            if (path.startsWith("/blog/")) {
                var post = blog.publicPost(path.substring(6));
                ogTitle = escape(nonblank(post.ogTitle(), nonblank(post.seoTitle(), post.title())));
                ogDescription = escape(nonblank(post.ogDescription(), nonblank(post.seoDescription(), description)));
            }
            String url = SITE + path;
            String head = "<title>" + cleanTitle + "</title>" +
                    "<meta name=\"description\" content=\"" + cleanDescription + "\">" +
                    "<link rel=\"canonical\" href=\"" + url + "\">" +
                    "<meta property=\"og:type\" content=\"website\">" +
                    "<meta property=\"og:title\" content=\"" + ogTitle + "\">" +
                    "<meta property=\"og:description\" content=\"" + ogDescription + "\">" +
                    "<meta property=\"og:url\" content=\"" + url + "\">" +
                    (ogImage != null && (ogImage.startsWith("https://") || ogImage.startsWith("/"))
                            ? "<meta property=\"og:image\" content=\"" + escape(ogImage.startsWith("/") ? SITE + ogImage : ogImage) + "\">" : "");
            if (schema != null) head += "<script type=\"application/ld+json\">" + json.writeValueAsString(schema).replace("</", "<\\/") + "</script>";
            String html = shell().replaceFirst("(?s)<title>.*?</title>", "")
                    .replaceFirst("(?s)<meta name=\"description\"[^>]*>", "")
                    .replace("<meta name=\"robots\" content=\"noindex\" />", "<meta name=\"robots\" content=\"" + (path.equals("/blog") && blog.publicPosts().isEmpty() ? "noindex,follow" : "index,follow") + "\">")
                    .replace("</head>", head + "</head>")
                    .replace("<div id=\"root\"></div>", "<div id=\"root\"><main><h1>" + escape(heading) + "</h1><p>" + cleanDescription + "</p>" + initialBody(path) + "</main></div>");
            return ResponseEntity.ok().cacheControl(CacheControl.noCache()).contentType(HTML).body(html);
        } catch (Exception ex) {
            log.error("Could not render SEO HTML for {}", path, ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).contentType(MediaType.TEXT_PLAIN).body("SEO page temporarily unavailable");
        }
    }

    private synchronized String shell() throws Exception {
        if (shell != null && Instant.now().isBefore(shellExpires)) return shell;
        var request = HttpRequest.newBuilder(shellUri).timeout(Duration.ofSeconds(5)).GET().build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || !response.body().contains("<div id=\"root\"></div>"))
            throw new IllegalStateException("Frontend HTML shell unavailable");
        shell = response.body();
        shellExpires = Instant.now().plusSeconds(60);
        return shell;
    }

    private String initialBody(String path) {
        if (path.equals("/")) {
            var content = presentation.current().content();
            var intro = content == null || content.pages() == null ? null :
                    content.pages().getOrDefault("home", Map.of()).get("introBody");
            return intro == null ? "" : "<section><p>" + escape(intro.vi()) +
                    "</p><a href=\"/san-pham\">Sản phẩm</a> <a href=\"/trai-nghiem\">Trải nghiệm</a></section>";
        }
        if (path.equals("/faq")) {
            StringBuilder html = new StringBuilder();
            faq.publicItems().forEach(item -> html.append("<section><h2>").append(escape(item.questionVi()))
                    .append("</h2><p>").append(escape(item.answerVi())).append("</p></section>"));
            return html.toString();
        }
        if (path.equals("/chinh-sach")) {
            var content = presentation.current().content();
            var fields = content == null || content.pages() == null ? Map.<String, com.bautruc.ecommerce.sitecontent.api.request.SitePresentationRequest.TextPair>of()
                    : content.pages().getOrDefault("policy", Map.of());
            StringBuilder html = new StringBuilder();
            for (String section : java.util.List.of("payment", "shipping", "refund", "experience")) {
                var label = fields.get(section + "Title");
                var body = fields.get(section + "Body");
                if (label != null && body != null) html.append("<section><h2>").append(escape(label.vi()))
                        .append("</h2><p>").append(escape(body.vi())).append("</p></section>");
            }
            var contact = support.current();
            html.append("<section><h2>Liên hệ</h2><p>").append(escape(contact.getAddress())).append(" · ")
                    .append(escape(contact.getEmail())).append(" · Zalo ").append(escape(contact.getZaloPhone()))
                    .append("</p></section>");
            return html.toString();
        }
        if (path.equals("/ve-chung-toi")) {
            var value = about.current().content();
            if (value == null) return "";
            return "<section><h2>" + aboutText(value.storyTitle()) + "</h2>" +
                    "<h2>" + aboutText(value.visionTitle()) + "</h2><p>" + aboutText(value.visionBody()) + "</p>" +
                    "<h2>" + aboutText(value.missionTitle()) + "</h2><p>" + aboutText(value.missionBody()) + "</p>" +
                    "<h2>" + aboutText(value.coreValuesTitle()) + "</h2><p>" + aboutText(value.coreValuesBody()) + "</p></section>";
        }
        if (path.equals("/san-pham") || path.startsWith("/san-pham/")) {
            Long groupId = path.equals("/san-pham") ? null : catalog.publicCollection(path.substring(10)).map(ProductCollection::getId).orElse(null);
            if (path.equals("/san-pham") || groupId != null) {
                String sql = "SELECT name_vi,slug FROM products WHERE status='ACTIVE' AND deleted_at IS NULL" +
                        (groupId == null ? "" : " AND collection_id = ?") + " ORDER BY id DESC LIMIT 20";
                var rows = groupId == null ? jdbc.query(sql, (rs, row) -> "<li><a href=\"/san-pham/" + rs.getString(2) + "\">" + escape(rs.getString(1)) + "</a></li>")
                        : jdbc.query(sql, (rs, row) -> "<li><a href=\"/san-pham/" + rs.getString(2) + "\">" + escape(rs.getString(1)) + "</a></li>", groupId);
                return "<ul>" + String.join("", rows) + "</ul>";
            }
        }
        if (path.equals("/trai-nghiem")) {
            var names = jdbc.query("SELECT title FROM workshop_offerings WHERE status='ACTIVE' ORDER BY id LIMIT 20",
                    (rs, row) -> "<li>" + escape(rs.getString(1)) + "</li>");
            return "<ul>" + String.join("", names) + "</ul>";
        }
        if (path.equals("/blog")) {
            var content = presentation.current().content();
            var subtitle = content == null || content.pages() == null
                    ? null
                    : content.pages().getOrDefault("blog", Map.of()).get("heroSubtitle");

            String intro = subtitle == null
                    ? ""
                    : "<p>" + escape(subtitle.vi()) + "</p>";
            return intro + "<ul>" + blog.publicPosts().stream().map(post -> "<li><a href=\"/blog/" + post.slug() + "\">" +
                    escape(post.title()) + "</a></li>").collect(java.util.stream.Collectors.joining()) + "</ul>";
        }
        if (path.startsWith("/blog/")) return markdownBody(blog.publicPost(path.substring(6)).bodyMarkdown());
        return "";
    }

    private static String markdownBody(String source) {
        StringBuilder html = new StringBuilder();
        boolean started = false;
        for (String line : source.split("\\R")) {
            if (line.startsWith("# ")) { started = true; continue; }
            if (!started || line.isBlank() || line.startsWith("[Ảnh:")) continue;
            if (line.startsWith("## ")) html.append("<h2>").append(escape(line.substring(3))).append("</h2>");
            else if (line.startsWith("### ")) html.append("<h3>").append(escape(line.substring(4))).append("</h3>");
            else html.append("<p>").append(escape(line)).append("</p>");
        }
        return html.toString();
    }

    private Object faqSchema() {
        var entries = new java.util.ArrayList<Map<String, Object>>();
        faq.publicItems().forEach(item -> entries.add(Map.of("@type", "Question", "name", item.questionVi(),
                "acceptedAnswer", Map.of("@type", "Answer", "text", item.answerVi()))));
        return entries.isEmpty() ? null : Map.of("@context", "https://schema.org", "@type", "FAQPage", "mainEntity", entries);
    }

    private static String aboutText(com.bautruc.ecommerce.sitecontent.api.request.AboutContentRequest.TextPair value) {
        return value == null ? "" : escape(value.vi());
    }

    private ResponseEntity<String> redirect(String path) {
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, SITE + path).build();
    }

    private ResponseEntity<String> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).header("X-Robots-Tag", "noindex")
                .contentType(HTML).body("<!doctype html><html lang=\"vi\"><meta charset=\"utf-8\"><title>Không tìm thấy trang</title><h1>Không tìm thấy trang</h1></html>");
    }

    private static String nonblank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String summary(String value) {
        if (value == null) return null;
        String clean = value.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        if (clean.length() <= 155) return clean;
        int boundary = clean.lastIndexOf(' ', 155);
        return clean.substring(0, boundary > 100 ? boundary : 155);
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value.replaceAll("<[^>]*>", " "));
    }
}
