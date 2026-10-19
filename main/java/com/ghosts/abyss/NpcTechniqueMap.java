package com.ghosts.abyss;

import java.util.HashMap;
import java.util.Map;

public class NpcTechniqueMap {
   private static final Map<String, Double> MAP = new HashMap();

   public static double get(Class<?> entityClass) {
      return (Double)MAP.getOrDefault(entityClass.getName(), (double)-1.0F);
   }

   public static boolean contains(Class<?> entityClass) {
      return MAP.containsKey(entityClass.getName());
   }

   static {
      String PKG = "net.mcreator.jujutsucraft.entity.";
      MAP.put("net.mcreator.jujutsucraft.entity.GojoSatoruEntity", (double)2.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.GojoSatoruSchoolDaysEntity", (double)2.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.FushiguroMegumiEntity", (double)6.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.FushiguroMegumiShibuyaEntity", (double)6.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.InumakiTogeEntity", (double)3.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.OkkotsuYutaEntity", (double)5.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.OkkotsuYutaCullingGameEntity", (double)5.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.HakariKinjiEntity", (double)29.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.TodoAoiEntity", (double)20.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.NanamiKentoEntity", (double)13.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.MeiMeiEntity", (double)11.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.TsukumoYukiEntity", (double)9.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.KamoNoritoshiEntity", (double)10.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.InoTakumaEntity", (double)40.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.YagaMasamichiEntity", (double)33.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.KusakabeAtsuyaEntity", (double)31.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.MiwaKasumiEntity", (double)31.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.NishimiyaMomoEntity", (double)36.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.GetoSuguruEntity", (double)18.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.GetoSuguruCurseUserEntity", (double)18.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.YoshinoJunpeiEntity", (double)35.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.ZeninNaoyaEntity", (double)19.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.ZeninNaobitoEntity", (double)19.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.ZeninJinichiEntity", (double)22.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.ZeninOgiEntity", (double)26.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.ZeninRantaEntity", (double)46.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.ZeninChojuroEntity", (double)32.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.TakabaFumihikoEntity", (double)17.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.HigurumaHiromiEntity", (double)27.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.IshigoriRyuEntity", (double)12.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.KashimoHajimeEntity", (double)7.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.HazenokiEntity", (double)45.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.ReggieStarEntity", (double)44.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.UraumeEntity", (double)24.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.NobaraKugisakiEntity", (double)34.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.ItadoriYujiEntity", (double)21.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.ItadoriYujiShibuyaEntity", (double)21.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.ItadoriYujiShinjukuEntity", (double)21.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.MiguelEntity", (double)30.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.MiguelDancerEntity", (double)30.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.RozetsuEntity", (double)43.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.DhruvLakdawallaEntity", (double)37.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.UroTakakoEntity", (double)38.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.YorozuEntity", (double)39.0F);
      MAP.put("net.mcreator.jujutsucraft.entity.KurusuHanaEntity", (double)28.0F);
   }
}
