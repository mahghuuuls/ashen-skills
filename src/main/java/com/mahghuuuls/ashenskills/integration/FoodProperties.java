package com.mahghuuuls.ashenskills.integration;

/** The food mod's side of the four attributes: whether pack food definitions can give their points. */
public interface FoodProperties {

    /** No food mod is installed: nothing gives food points. */
    FoodProperties NONE = new FoodProperties() {
        @Override
        public int registered() {
            return 0;
        }
    };

    /** How many properties the food mod took. */
    int registered();
}
