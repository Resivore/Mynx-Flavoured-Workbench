package com.fizzware.dramaticdoors.compat;

public interface CompatChecker
{
	/**
	 * Checks if the mod is loaded.
	 */
	default boolean isModLoaded(String modid) {
		return false;
	}
	
	/**
	 * Checks if it's development environment. On dev environment, enables many standard compats.
	 */
	default boolean isDev() {
		return false;
	}
	
	default boolean isQuarkModuleEnabled() {
		return false;
	}
}
