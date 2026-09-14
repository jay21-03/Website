import { useContext } from 'react'
import { useNavigate } from 'react-router-dom'
import { DesignContext } from './i18n'

export function useCart() {
  const { count } = useContext(DesignContext)
  const navigate = useNavigate()
  return { count, setOpen: open => { if (open) navigate('/cart') } }
}
