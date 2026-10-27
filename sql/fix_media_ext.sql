-- 修复 /media/goods 图片后缀：磁盘实际是 .jpg，早期数据里写成了 .png
-- 一次正则替换时捕获组丢失，导致出现 /media/goods/1.jpg，这里按 id / spu_id 还原

-- 1) 还原 spu.main_pictures（图片名规则：g{spu.id}.jpg）
UPDATE spu
   SET main_pictures = REPLACE(main_pictures, '/media/goods/1.jpg', CONCAT('/media/goods/g', id, '.jpg'))
 WHERE main_pictures LIKE '%/media/goods/1.jpg%';

-- 2) 还原 video.cover（封面跟商品走：g{video.spu_id}.jpg）
UPDATE video
   SET cover = CONCAT('/media/goods/g', spu_id, '.jpg')
 WHERE cover = '/media/goods/1.jpg' AND spu_id IS NOT NULL;

-- 3) 兜底：仍残留 .png 的（未被上面覆盖的）统一改 .jpg
UPDATE spu
   SET main_pictures = REPLACE(main_pictures, CONCAT('/media/goods/g', id, '.png'), CONCAT('/media/goods/g', id, '.jpg'))
 WHERE main_pictures LIKE '%/media/goods/%.png%';

UPDATE video
   SET cover = CONCAT('/media/goods/g', spu_id, '.jpg')
 WHERE cover LIKE '/media/goods/%.png' AND spu_id IS NOT NULL;

-- 4) 自检
SELECT 'spu_png_left' AS chk, COUNT(*) AS n FROM spu WHERE main_pictures LIKE '%/media/goods/%.png%'
UNION ALL
SELECT 'video_png_left', COUNT(*) FROM video WHERE cover LIKE '/media/goods/%.png'
UNION ALL
SELECT 'broken_1jpg', COUNT(*) FROM spu WHERE main_pictures LIKE '%/media/goods/1.jpg%';
