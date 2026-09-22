UPDATE support_settings
SET email = 'dangxembautrucpottery@gmail.com',
    updated_at = now()
WHERE id = 1
  AND email <> 'dangxembautrucpottery@gmail.com';
