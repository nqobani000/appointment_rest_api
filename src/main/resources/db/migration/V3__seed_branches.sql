BEGIN;

INSERT INTO public.provinces (code, name)
VALUES ('GAUTENG', 'Gauteng')
ON CONFLICT (code) DO NOTHING;

-- relax the schema so that we can insert the seed data without having to know the postal codes
ALTER TABLE public.locations
    ALTER COLUMN street_no DROP NOT NULL,
    ALTER COLUMN street_name DROP NOT NULL,
    ALTER COLUMN suburb DROP NOT NULL,
    ALTER COLUMN postal_code DROP NOT NULL;

ALTER TABLE public.locations
    ADD COLUMN IF NOT EXISTS latitude  double precision,
    ADD COLUMN IF NOT EXISTS longitude double precision;

-- safeguard against running this migration twice, which would otherwise fail due to the foreign key constraints already existing
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM pg_constraint
                       WHERE conname = 'fk_locations_province'
                         AND conrelid = 'public.locations'::regclass) THEN
            ALTER TABLE public.locations
                ADD CONSTRAINT fk_locations_province
                    FOREIGN KEY (province) REFERENCES public.provinces (code)
                        ON DELETE RESTRICT;
        END IF;
    END
$$;

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM pg_constraint
                       WHERE conname = 'fk_branches_location'
                         AND conrelid = 'public.branches'::regclass) THEN
            ALTER TABLE public.branches
                ADD CONSTRAINT fk_branches_location
                    FOREIGN KEY (location_id) REFERENCES public.locations (id)
                        ON DELETE RESTRICT;
        END IF;
    END
$$;


CREATE UNIQUE INDEX IF NOT EXISTS uq_locations_natural_key
    ON public.locations (
                         COALESCE(street_no, ''),
                         COALESCE(street_name, ''),
                         COALESCE(suburb, ''),
                         city,
                         province,
                         COALESCE(country, '')
        );

CREATE UNIQUE INDEX IF NOT EXISTS uq_branches_name
    ON public.branches (name);


CREATE TEMP TABLE seed_capitec_branches ON COMMIT DROP AS
SELECT x.name,
       NULLIF(x.location ->> 'streetNo', '')                                                 AS street_no,
       NULLIF(x.location ->> 'streetName', '')                                               AS street_name,
       NULLIF(x.location ->> 'suburb', '')                                                   AS suburb,
       x.location ->> 'city'                                                                 AS city,
       CASE x.location ->> 'province'
           WHEN 'Eastern Cape' THEN 'EASTERN_CAPE'
           WHEN 'Free State' THEN 'FREE_STATE'
           WHEN 'Gauteng' THEN 'GAUTENG'
           WHEN 'KwaZulu-Natal' THEN 'KWAZULU_NATAL'
           WHEN 'Limpopo' THEN 'LIMPOPO'
           WHEN 'Mpumalanga' THEN 'MPUMALANGA'
           WHEN 'Northern Cape' THEN 'NORTHERN_CAPE'
           WHEN 'North West' THEN 'NORTH_WEST'
           WHEN 'Western Cape' THEN 'WESTERN_CAPE'
           ELSE x.location ->> 'province'
           END                                                                               AS province,
       x.location ->> 'country'                                                              AS country,
       NULLIF(btrim(split_part(x.location ->> 'coordinates', ',', 1)), '')::double precision AS latitude,
       NULLIF(btrim(split_part(x.location ->> 'coordinates', ',', 2)), '')::double precision AS longitude
FROM jsonb_to_recordset(
             $seed$[
               {
                 "name": "Capitec Bank Tembisa Mayibuye",
                 "location": {
                   "streetNo": "Shop 2",
                   "streetName": "Mayibuye Centre, Modderfontein Road & Mastiff Ave",
                   "suburb": "Commercia",
                   "city": "Tembisa",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.0335324, 28.1754125"
                 }
               },
               {
                 "name": "Capitec Bank Vanderbijlpark CE 2",
                 "location": {
                   "streetNo": null,
                   "streetName": null,
                   "suburb": "Vanderbijlpark CE 2",
                   "city": "Vanderbijlpark",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.69762, 27.83742"
                 }
               },
               {
                 "name": "Capitec Bank Gleneagles",
                 "location": {
                   "streetNo": null,
                   "streetName": null,
                   "suburb": "Gleneagles",
                   "city": "Johannesburg",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.26909, 28.04928"
                 }
               },
               {
                 "name": "Capitec Bank Yeoville",
                 "location": {
                   "streetNo": null,
                   "streetName": null,
                   "suburb": "Yeoville",
                   "city": "Johannesburg",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.18110, 28.06275"
                 }
               },
               {
                 "name": "Capitec Bank Johannesburg Pritchard",
                 "location": {
                   "streetNo": "90-98",
                   "streetName": "Pritchard Street",
                   "suburb": "Johannesburg Central",
                   "city": "Johannesburg",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.2031266, 28.0458588"
                 }
               },
               {
                 "name": "Capitec Bank Pretoria Bougainville",
                 "location": {
                   "streetNo": "Shop 12",
                   "streetName": "Bougainville Shopping Centre, Jennings St",
                   "suburb": "Daspoort",
                   "city": "Pretoria",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-25.71763, 28.15308"
                 }
               },
               {
                 "name": "Capitec Bank Pretorius Street",
                 "location": {
                   "streetNo": "200",
                   "streetName": "Pretorius Street, Perm Building",
                   "suburb": "Pretoria Central",
                   "city": "Pretoria",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-25.74770, 28.18918"
                 }
               },
               {
                 "name": "Capitec Bank (Centurion area)",
                 "location": {
                   "streetNo": null,
                   "streetName": null,
                   "suburb": null,
                   "city": "Centurion",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-25.68102, 28.25197"
                 }
               },
               {
                 "name": "Capitec Bank Nigel",
                 "location": {
                   "streetNo": "23",
                   "streetName": "Hendrik Verwoerd Street",
                   "suburb": "Nigel",
                   "city": "Nigel",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.4180199, 28.46822"
                 }
               },
               {
                 "name": "Capitec Bank Alexandra (Hofmeyr St)",
                 "location": {
                   "streetNo": "5543",
                   "streetName": "Hofmeyr Street",
                   "suburb": "Alexandra",
                   "city": "Johannesburg",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.1033, 28.0976"
                 }
               },
               {
                 "name": "Capitec Bank Katlehong Letsoho",
                 "location": {
                   "streetNo": "Shop N5 & N6",
                   "streetName": "Letsoho Shopping Centre, Cnr Hospital Road",
                   "suburb": "Katlehong",
                   "city": "Katlehong",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.333, 28.150"
                 }
               },
               {
                 "name": "Capitec Bank Alberton Promenade",
                 "location": {
                   "streetNo": "Shop 1",
                   "streetName": "Promenade Centre, 38 Alberton Blvd",
                   "suburb": "Alberton",
                   "city": "Alberton",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.267, 28.117"
                 }
               },
               {
                 "name": "Capitec Bank Silverton Silverwater Crossing",
                 "location": {
                   "streetNo": "Shop 4D & 3A",
                   "streetName": "Silverwater Crossing",
                   "suburb": "Silverton",
                   "city": "Pretoria",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-25.733, 28.283"
                 }
               },
               {
                 "name": "Capitec Bank Vereeniging Voortrekker",
                 "location": {
                   "streetNo": "Shop 2 & 3",
                   "streetName": "Voortrekker Street",
                   "suburb": "Vereeniging",
                   "city": "Vereeniging",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.673, 27.926"
                 }
               },
               {
                 "name": "Capitec Bank Pretoria Silver Lakes",
                 "location": {
                   "streetNo": "Shop 2",
                   "streetName": "Silver Lakes",
                   "suburb": "Silver Lakes",
                   "city": "Pretoria",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-25.783, 28.283"
                 }
               },
               {
                 "name": "Capitec Bank Orange Farm Eyethu Mall",
                 "location": {
                   "streetNo": "L15",
                   "streetName": "Eyethu Mall",
                   "suburb": "Orange Farm",
                   "city": "Orange Farm",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.483, 27.833"
                 }
               },
               {
                 "name": "Capitec Bank Carletonville Mall",
                 "location": {
                   "streetNo": null,
                   "streetName": "Carletonville Mall",
                   "suburb": "Carletonville",
                   "city": "Carletonville",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.360, 27.397"
                 }
               },
               {
                 "name": "Capitec Bank Sandton City",
                 "location": {
                   "streetNo": "L14f",
                   "streetName": "Sandton City Shopping Centre",
                   "suburb": "Sandton",
                   "city": "Johannesburg",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.107, 28.056"
                 }
               },
               {
                 "name": "Capitec Bank Florida Goldman Crossing",
                 "location": {
                   "streetNo": null,
                   "streetName": "Goldman Crossing",
                   "suburb": "Florida",
                   "city": "Roodepoort",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.183, 27.933"
                 }
               },
               {
                 "name": "Capitec Bank Eldorado Park",
                 "location": {
                   "streetNo": null,
                   "streetName": null,
                   "suburb": "Eldorado Park",
                   "city": "Johannesburg",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.283, 27.933"
                 }
               },
               {
                 "name": "Capitec Bank Pretoria Hatfield",
                 "location": {
                   "streetNo": null,
                   "streetName": null,
                   "suburb": "Hatfield",
                   "city": "Pretoria",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-25.747, 28.233"
                 }
               },
               {
                 "name": "Capitec Bank Lenasia Square",
                 "location": {
                   "streetNo": null,
                   "streetName": "Lenasia Square",
                   "suburb": "Lenasia",
                   "city": "Lenasia",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.316, 27.833"
                 }
               },
               {
                 "name": "Capitec Bank Tembisa Birch Acres",
                 "location": {
                   "streetNo": null,
                   "streetName": null,
                   "suburb": "Birch Acres",
                   "city": "Tembisa",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.033, 28.233"
                 }
               },
               {
                 "name": "Capitec Bank Heidelberg Mall",
                 "location": {
                   "streetNo": null,
                   "streetName": "Heidelberg Mall",
                   "suburb": "Heidelberg",
                   "city": "Heidelberg",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.500, 28.350"
                 }
               },
               {
                 "name": "Capitec Bank Lenasia Trade Route Mall",
                 "location": {
                   "streetNo": null,
                   "streetName": "Trade Route Mall",
                   "suburb": "Lenasia",
                   "city": "Lenasia",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.316, 27.833"
                 }
               },
               {
                 "name": "Capitec Bank Mamelodi Square",
                 "location": {
                   "streetNo": null,
                   "streetName": "Mamelodi Square",
                   "suburb": "Mamelodi",
                   "city": "Pretoria",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-25.700, 28.400"
                 }
               },
               {
                 "name": "Capitec Bank Soshanguve Mall",
                 "location": {
                   "streetNo": "V15",
                   "streetName": "Soshanguve Mall",
                   "suburb": "Soshanguve",
                   "city": "Pretoria",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-25.533, 28.100"
                 }
               },
               {
                 "name": "Capitec Bank Soweto Bara Mall",
                 "location": {
                   "streetNo": null,
                   "streetName": "Bara Mall",
                   "suburb": "Soweto",
                   "city": "Soweto",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.233, 27.933"
                 }
               },
               {
                 "name": "Capitec Bank Springs Mall",
                 "location": {
                   "streetNo": null,
                   "streetName": "Springs Mall",
                   "suburb": "Springs",
                   "city": "Springs",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.250, 28.433"
                 }
               },
               {
                 "name": "Capitec Bank Tembisa Plaza",
                 "location": {
                   "streetNo": null,
                   "streetName": "Tembisa Plaza",
                   "suburb": "Tembisa",
                   "city": "Tembisa",
                   "province": "Gauteng",
                   "country": "South Africa",
                   "coordinates": "-26.033, 28.233"
                 }
               }
             ]$seed$::jsonb
     ) AS x(name varchar(150), location jsonb);


INSERT INTO public.locations
(street_no, street_name, suburb, city, province, postal_code, country, latitude, longitude)
SELECT DISTINCT ON (
    COALESCE(s.street_no, ''),
    COALESCE(s.street_name, ''),
    COALESCE(s.suburb, ''),
    s.city,
    s.province,
    COALESCE(s.country, '')
    ) s.street_no,
      s.street_name,
      s.suburb,
      s.city,
      s.province,
      NULL::varchar AS postal_code,
      s.country,
      s.latitude,
      s.longitude
FROM seed_capitec_branches s
ORDER BY COALESCE(s.street_no, ''),
         COALESCE(s.street_name, ''),
         COALESCE(s.suburb, ''),
         s.city,
         s.province,
         COALESCE(s.country, '')
ON CONFLICT (
    COALESCE(street_no, ''),
    COALESCE(street_name, ''),
    COALESCE(suburb, ''),
    city,
    province,
    COALESCE(country, '')
    )
    DO UPDATE SET latitude  = EXCLUDED.latitude,
                  longitude = EXCLUDED.longitude;


INSERT INTO public.branches (name, location_id)
SELECT s.name, l.id
FROM seed_capitec_branches s
         JOIN public.locations l
              ON COALESCE(l.street_no, '') = COALESCE(s.street_no, '')
                  AND COALESCE(l.street_name, '') = COALESCE(s.street_name, '')
                  AND COALESCE(l.suburb, '') = COALESCE(s.suburb, '')
                  AND l.city = s.city
                  AND l.province = s.province
                  AND COALESCE(l.country, '') = COALESCE(s.country, '')
ON CONFLICT (name) DO UPDATE SET location_id = EXCLUDED.location_id;

COMMIT;