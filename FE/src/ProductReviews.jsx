import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './api'

const stars = rating => {
  const count = Math.max(0, Math.min(5, Number(rating) || 0))
  return '★'.repeat(count) + '☆'.repeat(5 - count)
}

export function ProductReviews({ productId, user, lang = 'vi' }) {
  const client = useQueryClient()
  const [page, setPage] = useState(0)
  const [draft, setDraft] = useState({ productId: null, body: null, rating: null })
  const [message, setMessage] = useState('')
  const [saving, setSaving] = useState(false)
  const tr = (vi, en) => lang === 'en' ? en : vi

  const list = useQuery({
    queryKey: ['product-reviews', productId, page],
    queryFn: () => api.productReviews(productId, page),
    enabled: Boolean(productId)
  })

  const mine = useQuery({
    queryKey: ['my-product-review', user?.id ?? null, productId],
    queryFn: () => api.myProductReview(productId),
    enabled: Boolean(productId && user?.role === 'USER')
  })

  const currentDraft = draft.productId === productId ? draft : null
  const body = currentDraft?.body ?? mine.data?.review?.body ?? ''
  const rating = currentDraft?.rating ?? mine.data?.review?.rating ?? 5
  const reviews = list.data?.reviews

  async function submit(event) {
    event.preventDefault()
    if (saving) return

    const text = body.trim()
    if (text.length < 10 || text.length > 1000) {
      setMessage(tr(
        'Nhận xét cần từ 10 đến 1000 ký tự.',
        'Review must contain 10 to 1000 characters.'
      ))
      return
    }

    setSaving(true)
    setMessage('')
    try {
      const payload = { rating: Number(rating), body: text }
      if (mine.data?.review) {
        await api.updateProductReview(productId, payload)
      } else {
        await api.createProductReview(productId, payload)
      }

      setDraft({ productId, rating: payload.rating, body: payload.body })
      await Promise.all([
        client.invalidateQueries({ queryKey: ['product-reviews', productId] }),
        client.invalidateQueries({ queryKey: ['my-product-review', user?.id ?? null, productId] })
      ])
      setMessage(tr('Đã lưu đánh giá.', 'Review saved.'))
    } catch (error) {
      setMessage(error.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="product-reviews">
      <h2>{tr('Đánh giá khách hàng', 'Customer reviews')}</h2>

      <div className="review-summary">
        <strong>{Number(list.data?.averageRating ?? 0).toFixed(1)} / 5</strong>
        <span className="review-stars" aria-hidden="true">★★★★★</span>
        <span>
          {list.data?.ratingCount ?? 0} {tr('đánh giá', 'reviews')}
        </span>
      </div>

      {list.isError && (
        <p role="alert">
          {list.error.message}{' '}
          <button type="button" onClick={() => list.refetch()}>
            {tr('Thử lại', 'Retry')}
          </button>
        </p>
      )}

      {user?.role === 'USER' && mine.data?.eligible && (
        <form className="review-form" onSubmit={submit}>
          <h3>
            {mine.data.review
              ? tr('Sửa đánh giá', 'Edit review')
              : tr('Viết đánh giá', 'Write a review')}
          </h3>

          <select
            aria-label={tr('Số sao đánh giá', 'Review rating')}
            value={rating}
            onChange={event => setDraft({
              productId,
              body,
              rating: Number(event.target.value)
            })}
            disabled={saving}
          >
            {[5, 4, 3, 2, 1].map(value => (
              <option key={value} value={value}>{stars(value)}</option>
            ))}
          </select>

          <textarea
            aria-label={tr('Nội dung đánh giá', 'Review text')}
            value={body}
            onChange={event => setDraft({
              productId,
              body: event.target.value,
              rating
            })}
            minLength={10}
            maxLength={1000}
            required
            rows={5}
            disabled={saving}
          />

          <button type="submit" className="button dark" disabled={saving}>
            {saving
              ? tr('Đang lưu...', 'Saving...')
              : tr('Lưu đánh giá', 'Save review')}
          </button>

          {message && <p role="status">{message}</p>}
        </form>
      )}

      <div className="review-list">
        {(reviews?.content ?? []).map(review => (
          <article className="review-card" key={review.id}>
            <strong className="review-stars" aria-label={`${review.rating}/5`}>
              {stars(review.rating)}
            </strong>
            <p>{review.body}</p>
          </article>
        ))}
      </div>

      {reviews?.totalPages > 1 && (
        <nav className="review-pagination" aria-label={tr('Trang đánh giá', 'Review pages')}>
          <button
            type="button"
            aria-label={tr('Trang trước', 'Previous page')}
            disabled={reviews.first}
            onClick={() => setPage(value => Math.max(0, value - 1))}
          >
            ‹
          </button>
          <span>{page + 1}/{reviews.totalPages}</span>
          <button
            type="button"
            aria-label={tr('Trang sau', 'Next page')}
            disabled={reviews.last}
            onClick={() => setPage(value => value + 1)}
          >
            ›
          </button>
        </nav>
      )}
    </section>
  )
}

export function AdminProductReviews({ lang = 'vi', notify }) {
  const client = useQueryClient()
  const [page, setPage] = useState(0)
  const [busyId, setBusyId] = useState(null)
  const tr = (vi, en) => lang === 'en' ? en : vi

  const query = useQuery({
    queryKey: ['admin-product-reviews', page],
    queryFn: () => api.adminReviews(page)
  })

  async function toggle(review) {
    if (busyId !== null) return

    setBusyId(review.id)
    try {
      await api.setReviewHidden(review.id, !review.hidden)
      await Promise.all([
        client.invalidateQueries({ queryKey: ['admin-product-reviews'] }),
        client.invalidateQueries({ queryKey: ['product-reviews', review.productId] })
      ])
      notify(tr(
        'Đã cập nhật trạng thái đánh giá.',
        'Review visibility updated.'
      ))
    } catch (error) {
      notify(error.message)
    } finally {
      setBusyId(null)
    }
  }

  return (
    <section className="admin-panel">
      <h2>{tr('Quản lý đánh giá', 'Review management')}</h2>

      {query.isLoading && <p>{tr('Đang tải...', 'Loading...')}</p>}

      {query.isError && (
        <div role="alert">
          <p>{query.error.message}</p>
          <button type="button" onClick={() => query.refetch()}>
            {tr('Thử lại', 'Retry')}
          </button>
        </div>
      )}

      {(query.data?.content ?? []).map(review => (
        <article className="admin-review-card" key={review.id}>
          <p>#{review.productId} · USER #{review.userId}</p>
          <div className="review-stars" aria-label={`${review.rating}/5`}>
            {stars(review.rating)}
          </div>
          <p>{review.body}</p>
          <p>
            {review.hidden
              ? tr('Đang ẩn', 'Hidden')
              : tr('Đang hiển thị', 'Visible')}
          </p>
          <button
            type="button"
            disabled={busyId !== null}
            onClick={() => toggle(review)}
          >
            {review.hidden
              ? tr('Hiện đánh giá', 'Show review')
              : tr('Ẩn đánh giá', 'Hide review')}
          </button>
        </article>
      ))}

      {query.data && query.data.totalPages > 1 && (
        <nav className="review-pagination" aria-label={tr('Trang đánh giá', 'Review pages')}>
          <button
            type="button"
            disabled={query.data.first}
            onClick={() => setPage(value => Math.max(0, value - 1))}
          >
            {tr('Trước', 'Previous')}
          </button>
          <span>{page + 1}/{query.data.totalPages}</span>
          <button
            type="button"
            disabled={query.data.last}
            onClick={() => setPage(value => value + 1)}
          >
            {tr('Sau', 'Next')}
          </button>
        </nav>
      )}
    </section>
  )
}
