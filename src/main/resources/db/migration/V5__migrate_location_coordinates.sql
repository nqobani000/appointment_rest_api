UPDATE public.locations
SET coordinates = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
WHERE coordinates IS NULL
  AND latitude IS NOT NULL
  AND longitude IS NOT NULL;

ALTER TABLE public.locations
	DROP COLUMN IF EXISTS latitude,
	DROP COLUMN IF EXISTS longitude;
