package com.prime.optimize;

import net.fabricmc.api.ClientModInitializer;

public final class PrimeOptimizeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PrimeOptimizeConfig.load();
    }
}
