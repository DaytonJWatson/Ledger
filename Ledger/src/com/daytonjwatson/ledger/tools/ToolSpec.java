package com.daytonjwatson.ledger.tools;

public final class ToolSpec {
	private final ToolVendorService.ToolType type;
	private final ToolVendorService.ToolTier tier;
	private final ToolVendorService.ToolVariant variant;

	public ToolSpec(ToolVendorService.ToolType type, ToolVendorService.ToolTier tier, ToolVendorService.ToolVariant variant) {
		this.type = type;
		this.tier = tier;
		this.variant = variant;
	}

	public ToolVendorService.ToolType getType() {
		return type;
	}

	public ToolVendorService.ToolTier getTier() {
		return tier;
	}

	public ToolVendorService.ToolVariant getVariant() {
		return variant;
	}
}
