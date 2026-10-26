-- Vitamina A em RAE (coluna própria da TACO) e vitamina B12 (USDA FoodData Central, só nos alimentos
-- comuns). Mesma lista de data/catalog/nutrients.csv, conferida pelo NutrientCatalogTest.
INSERT INTO nutrients (code, name_pt, unit, display_order) VALUES
    ('VITA_RAE', 'Vitamina A (RAE)', 'µg', 25),
    ('VITB12', 'Vitamina B12', 'µg', 26);
