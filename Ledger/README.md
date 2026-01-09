# Ledger

## Price generation
Prices in `prices.yml` are generated from price bands and then adjusted by deterministic processing multipliers that reward more processed items (crafting, smelting, cooking, etc.). Overrides in `overrides.yml` still take precedence over generated values.

To debug generation, set `prices.generator.processingDebug` in `config.yml` to `true` before running `/ledger genprices` to log the material, band, base, multiplier, and final price.
