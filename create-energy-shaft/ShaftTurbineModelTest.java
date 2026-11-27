import com.hbm.tileentity.machine.ShaftTurbineModel;
import com.hbm.tileentity.machine.ShaftTurbineModel.*;
import com.hbm.tileentity.machine.HeatExchangerModel;

public class ShaftTurbineModelTest {
    static int fails = 0;
    static void check(boolean ok, String what) { if (!ok) { fails++; System.out.println("FAIL " + what); } else System.out.println("ok   " + what); }

    static final long FULLP = ShaftTurbineModel.MAX_POWER;
    static Inputs in(long power, long inflow, int steam, int water) { return new Inputs(power, inflow, steam, water, 20, 100000); }

    /** a player who keeps nudging the water setting to the balance point */
    static void tune(ShaftTurbineModel m, double steamShare) {
        m.minWaterTemp = ShaftTurbineModel.clampMinTemp((int) Math.round(ShaftTurbineModel.idealTemp(m.load, steamShare)));
    }

    static Outputs run(ShaftTurbineModel m, int ticks, long power, int steamMb, boolean tuned) {
        Outputs o = null;
        for (int t = 0; t < ticks; t++) {
            if (tuned) tune(m, Math.min(steamMb, ShaftTurbineModel.STEAM_MB_MAX * m.steamLimitPercent / 100D) / ShaftTurbineModel.STEAM_MB_MAX);
            o = m.tick(in(power, 1_000_000, steamMb, 100000));
            if (o.explode()) { System.out.println("     exploded at tick " + t); return o; }
        }
        return o;
    }

    public static void main(String[] a) {
        ShaftTurbineModel m = new ShaftTurbineModel(); Outputs o;

        o = run(m, 4000, FULLP, 0, true);
        check(o.su() <= 5_000_000 && o.su() >= 4_900_000, "tuned, full power: about 5M SU and never above: " + o.su());
        check(o.rpm() == 256, "rpm tops at 256: " + o.rpm());
        check(m.stability > ShaftTurbineModel.STABLE_AT, "tuned turbine reads stable: " + m.stability);

        m = new ShaftTurbineModel(); o = run(m, 4000, 150_000_000L, 0, true);
        check(o.su() > 2_300_000 && o.su() < 2_550_000, "half stored power gives about half SU: " + o.su());

        m = new ShaftTurbineModel(); m.throttlePercent = 40; o = run(m, 4000, FULLP, 0, true);
        check(o.su() > 1_900_000 && o.su() <= 2_050_000, "40% throttle gives about 2M SU: " + o.su());

        // an untuned turbine with no steam derates but never goes critical
        m = new ShaftTurbineModel(); m.minWaterTemp = 40; o = run(m, 6000, FULLP, 0, false);
        check(!o.explode() && o.alarm() < 2 && m.stability >= 0.34 && o.su() > 0, "untuned without steam derates, never critical: stab " + m.stability + " su " + o.su());

        // steam: more stress for the same stored power, and less power drawn per SU
        ShaftTurbineModel none = new ShaftTurbineModel(), some = new ShaftTurbineModel();
        some.steamLimitPercent = 50;
        Outputs on = run(none, 5000, 150_000_000L, 5000, true), os = run(some, 5000, 150_000_000L, 5000, true);
        check(os.su() > on.su() * 1.3, "50% steam lifts output: " + os.su() + " vs " + on.su());
        check(os.heUsed() / (double) os.su() < on.heUsed() / (double) on.su() * 0.85, "steam lowers power drawn per SU");
        check(some.stability > ShaftTurbineModel.STABLE_AT, "50% steam, tuned: stable " + some.stability);
        check(os.steamUsed() > 0 && on.steamUsed() == 0, "steam is consumed only when admitted");

        // steam is capped by the limit even with a full tank
        ShaftTurbineModel cap = new ShaftTurbineModel(); cap.steamLimitPercent = 25;
        o = run(cap, 3000, FULLP, 100000, true);
        check(o.steamUsed() <= ShaftTurbineModel.STEAM_MB_MAX * 25 / 100, "steam limit caps intake: " + o.steamUsed());

        // full steam, well tuned: dangerous but survivable. full steam, badly tuned: explodes
        ShaftTurbineModel tuned = new ShaftTurbineModel(); tuned.steamLimitPercent = 100;
        o = run(tuned, 6000, FULLP, 100000, true);
        check(!o.explode() && tuned.stability > 0.4 && tuned.stability < ShaftTurbineModel.STABLE_AT, "full steam tuned: strained but alive: " + tuned.stability);
        ShaftTurbineModel bad = new ShaftTurbineModel(); bad.steamLimitPercent = 100; bad.minWaterTemp = 40;
        o = run(bad, 6000, FULLP, 100000, false);
        check(o.explode(), "full steam badly tuned explodes");
        check(bad.critTicks >= ShaftTurbineModel.EXPLODE_TICKS, "explosion follows the critical countdown");

        // the player can save it: cut the steam during the countdown
        ShaftTurbineModel save = new ShaftTurbineModel(); save.steamLimitPercent = 100; save.minWaterTemp = 40;
        boolean blew = false;
        for (int t = 0; t < 3000 && !blew; t++) {
            if (t == 120) save.steamLimitPercent = 0;
            blew = save.tick(in(FULLP, 1_000_000, 100000, 100000)).explode();
        }
        check(!blew && save.critTicks < ShaftTurbineModel.EXPLODE_TICKS, "cutting steam in time prevents the explosion");

        // warning comes before the explosion
        ShaftTurbineModel w = new ShaftTurbineModel(); w.steamLimitPercent = 100; w.minWaterTemp = 40;
        int firstWarn = -1, firstBoom = -1;
        for (int t = 0; t < 3000 && firstBoom < 0; t++) {
            Outputs x = w.tick(in(FULLP, 1_000_000, 100000, 100000));
            if (x.alarm() >= 1 && firstWarn < 0) firstWarn = t;
            if (x.explode()) firstBoom = t;
        }
        check(firstWarn >= 0 && firstBoom - firstWarn > 100, "player gets a warning window before the blast: " + firstWarn + " -> " + firstBoom);

        // water limit settles at what the water can sustain
        m = new ShaftTurbineModel(); run(m, 4000, FULLP, 0, true);
        for (int t = 0; t < 600; t++) { tune(m, 0); o = m.tick(in(FULLP, 1_000_000, 0, 60)); }
        double sustain = 60D / ShaftTurbineModel.waterWanted(1D, 20, m.minWaterTemp);
        check(o.coolingLimited() && Math.abs(m.load - sustain) < 0.01, "60 mB of water settles at what it sustains: " + m.load + " vs " + sustain);
        check(ShaftTurbineModel.waterWanted(1, 20, 90) < ShaftTurbineModel.waterWanted(1, 20, 50), "higher min temp uses less water");

        // stops when power runs out, publishes the zero at once
        m = new ShaftTurbineModel(); run(m, 4000, FULLP, 0, true);
        boolean zero = false;
        for (int t = 0; t < 400; t++) { o = m.tick(in(0, 0, 0, 100000)); if (o.publish() && o.su() == 0) zero = true; }
        check(o.su() == 0 && o.rpm() == 0 && zero, "no power stops output and publishes zero");

        // network updates are rate limited
        m = new ShaftTurbineModel(); int publishes = 0;
        for (int t = 0; t < 2000; t++) { tune(m, 0); if (m.tick(in(FULLP, 1_000_000, 0, 100000)).publish()) publishes++; }
        check(publishes <= 2000 / ShaftTurbineModel.PUBLISH_COOLDOWN_TICKS + 2, "publishes at most once per 20 ticks: " + publishes);

        // bursty supply gives less output than a steady one
        ShaftTurbineModel steady = new ShaftTurbineModel(), bursty = new ShaftTurbineModel();
        for (int t = 0; t < 6000; t++) {
            tune(steady, 0); tune(bursty, 0);
            steady.tick(in(FULLP, 1_000_000, 0, 100000));
            bursty.tick(in(FULLP, t % 10 < 3 ? 1_000_000 : 0, 0, 100000));
        }
        check(bursty.load < steady.load * 0.9, "bursty power gives less output: " + bursty.load + " vs " + steady.load);

        check(ShaftTurbineModel.quantizeSu(1.0) == 5_000_000 && ShaftTurbineModel.quantizeSu(0.5017) % 50_000 == 0, "SU is quantized");
        check(ShaftTurbineModel.rpmFor(0.005) == 0 && ShaftTurbineModel.rpmFor(0.02) == 8 && ShaftTurbineModel.rpmFor(1.0) == 256, "rpm steps of 8, 0 below 1%");
        check(ShaftTurbineModel.clampMinTemp(5) == 40 && ShaftTurbineModel.clampMinTemp(500) == 95, "min temp clamped");
        check(ShaftTurbineModel.idealTemp(1, 0) == 90 && ShaftTurbineModel.idealTemp(0, 0) == 45, "balance point runs 45 to 90 C");

        // vertical heat exchanger
        check(HeatExchangerModel.ops(5000, 0, 9999, 9999) == 0, "no pumped water: nothing exchanged");
        check(HeatExchangerModel.ops(0, 5000, 9999, 9999) == 0, "no hot water: nothing exchanged");
        check(HeatExchangerModel.ops(5000, 5000, 100, 9999) == 100, "full cold output blocks the exchange: 100");
        check(HeatExchangerModel.ops(50000, 50000, 99999, 99999) == HeatExchangerModel.RATE, "exchange is capped per tick");
        check(HeatExchangerModel.ops(300, 800, 9999, 9999) == 300, "limited by the scarcer side");

        System.out.println(fails == 0 ? "ALL PASS" : fails + " FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }
}
