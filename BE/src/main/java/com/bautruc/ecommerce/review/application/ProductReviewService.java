package com.bautruc.ecommerce.review.application;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.response.PageResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.review.api.request.ReviewRequest;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductReviewService {
 public record ReviewView(Long id, Long productId, int rating, String body, Instant createdAt, Instant updatedAt) {}
 public record AdminReviewView(Long id, Long productId, Long userId, int rating, String body, boolean hidden, Instant createdAt, Instant updatedAt) {}
 public record ReviewPage(double averageRating, long ratingCount, PageResponse<ReviewView> reviews) {}
 public record Mine(boolean eligible, ReviewView review, boolean hidden) {}
 private final JdbcTemplate jdbc; private final BusinessClock clock;
 public ProductReviewService(JdbcTemplate jdbc, BusinessClock clock) { this.jdbc=jdbc; this.clock=clock; }
 @Transactional(readOnly=true) public ReviewPage publicReviews(Long productId,int page,int size) { requireProduct(productId); int p=Math.max(0,page), s=Math.min(100,Math.max(1,size)); Long count=jdbc.queryForObject("SELECT COUNT(*) FROM product_reviews WHERE product_id=? AND hidden=FALSE",Long.class,productId); Double avg=jdbc.queryForObject("SELECT COALESCE(AVG(rating),0) FROM product_reviews WHERE product_id=? AND hidden=FALSE",Double.class,productId); List<ReviewView> rows=jdbc.query("SELECT id,product_id,rating,body,created_at,updated_at FROM product_reviews WHERE product_id=? AND hidden=FALSE ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",this::mapPublic,productId,s,(long)p*s); long total=count==null?0:count; return new ReviewPage(avg==null?0:avg,total,PageResponse.from(new PageImpl<>(rows,PageRequest.of(p,s),total))); }
 @Transactional(readOnly=true) public Mine mine(Long userId,Long productId) { requireProduct(productId); List<AdminReviewView> rows=jdbc.query("SELECT id,product_id,user_id,rating,body,hidden,created_at,updated_at FROM product_reviews WHERE product_id=? AND user_id=?",this::mapAdmin,productId,userId); AdminReviewView r=rows.isEmpty()?null:rows.get(0); return new Mine(eligible(userId,productId),r==null?null:publicView(r),r!=null&&r.hidden()); }
 @Transactional public ReviewView create(Long userId,Long productId,ReviewRequest req) { requireProduct(productId); requireEligible(userId,productId); Instant now=clock.now(); List<Long> ids=jdbc.query("INSERT INTO product_reviews(product_id,user_id,rating,body,hidden,created_at,updated_at) VALUES(?,?,?, ?,FALSE,?,?) ON CONFLICT(user_id,product_id) DO NOTHING RETURNING id",(rs,i)->rs.getLong(1),productId,userId,req.rating(),normalize(req.body()),Timestamp.from(now),Timestamp.from(now)); if(ids.isEmpty()) throw error("REVIEW_ALREADY_EXISTS","Review already exists.",HttpStatus.CONFLICT); return mine(userId,productId).review(); }
 @Transactional public ReviewView update(Long userId,Long productId,ReviewRequest req) { requireProduct(productId); requireEligible(userId,productId); int n=jdbc.update("UPDATE product_reviews SET rating=?,body=?,updated_at=? WHERE product_id=? AND user_id=?",req.rating(),normalize(req.body()),Timestamp.from(clock.now()),productId,userId); if(n==0) throw error("REVIEW_NOT_FOUND","Review not found.",HttpStatus.NOT_FOUND); return mine(userId,productId).review(); }
 @Transactional(readOnly=true) public PageResponse<AdminReviewView> adminReviews(int page,int size) { int p=Math.max(0,page),s=Math.min(100,Math.max(1,size)); Long count=jdbc.queryForObject("SELECT COUNT(*) FROM product_reviews",Long.class); List<AdminReviewView> rows=jdbc.query("SELECT id,product_id,user_id,rating,body,hidden,created_at,updated_at FROM product_reviews ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",this::mapAdmin,s,(long)p*s); return PageResponse.from(new PageImpl<>(rows,PageRequest.of(p,s),count==null?0:count)); }
 @Transactional public AdminReviewView setHidden(Long id,boolean hidden) { int n=jdbc.update("UPDATE product_reviews SET hidden=?,updated_at=? WHERE id=?",hidden,Timestamp.from(clock.now()),id); if(n==0) throw error("REVIEW_NOT_FOUND","Review not found.",HttpStatus.NOT_FOUND); return jdbc.queryForObject("SELECT id,product_id,user_id,rating,body,hidden,created_at,updated_at FROM product_reviews WHERE id=?",this::mapAdmin,id); }
 private boolean eligible(Long u,Long p){ return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM orders o JOIN order_items oi ON oi.order_id=o.id JOIN payments pay ON pay.order_id=o.id WHERE o.user_id=? AND oi.product_id=? AND o.status='COMPLETED' AND pay.status='PAID')",Boolean.class,u,p)); }
 private void requireEligible(Long u,Long p){if(!eligible(u,p))throw error("REVIEW_NOT_ELIGIBLE","Only customers with completed and paid orders can review.",HttpStatus.FORBIDDEN);}
 private void requireProduct(Long p){Long n=jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE id=? AND status='ACTIVE' AND deleted_at IS NULL",Long.class,p);if(n==null||n==0)throw error("PRODUCT_NOT_FOUND","Product not found.",HttpStatus.NOT_FOUND);}
 private String normalize(String b){String s=b==null?"":b.trim();if(s.length()<10||s.length()>1000)throw error("VALIDATION_FAILED","Review must contain 10 to 1000 characters.",HttpStatus.BAD_REQUEST);return s;}
 private BusinessException error(String c,String m,HttpStatus s){return new BusinessException(c,m,s);}
 private ReviewView mapPublic(ResultSet r,int i)throws SQLException{return new ReviewView(r.getLong("id"),r.getLong("product_id"),r.getInt("rating"),r.getString("body"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant());}
 private AdminReviewView mapAdmin(ResultSet r,int i)throws SQLException{return new AdminReviewView(r.getLong("id"),r.getLong("product_id"),r.getLong("user_id"),r.getInt("rating"),r.getString("body"),r.getBoolean("hidden"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant());}
 private ReviewView publicView(AdminReviewView r){return new ReviewView(r.id(),r.productId(),r.rating(),r.body(),r.createdAt(),r.updatedAt());}
}
