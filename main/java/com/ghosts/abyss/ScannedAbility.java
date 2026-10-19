package com.ghosts.abyss;

public record ScannedAbility(int ct, int select, String name, double cost, boolean physical, boolean passive, boolean available) {
   public static final ScannedAbility UNBOUND = new ScannedAbility(-999, -999, "— Unbound —", (double)0.0F, false, false, false);

   public boolean isUnbound() {
      return this.ct == -999 && this.select == -999;
   }

   public KeybindConfig.Ability toSlotAbility() {
      return new KeybindConfig.Ability(this.ct, this.select, this.name);
   }
}
