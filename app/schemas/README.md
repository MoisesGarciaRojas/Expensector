Room schema exports are stored here.

Future database changes should add explicit `Migration` objects from the previous schema
version to the new schema version and include migration tests before shipping. Production
configuration must not use destructive migration fallback.
