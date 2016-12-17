package com.aricneto.twistytimer.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.aricneto.twistify.R;

import java.util.HashMap;
import java.util.Map;

import static android.support.v4.content.ContextCompat.getDrawable;

/**
 * Used by the alg list
 */
public class AlgUtils {
    /**
     * Gets a map that maps color letters to color values. The mapping is
     * suitable for 6-sided puzzles. Colors are defined for the letters 'Y'
     * (yellow), 'R' (red), 'G' (green), 'B' (blue), 'O' (orange), 'W'
     * (white), and 'N' (neutral). The neutral color can be used when the
     * color is not important, e.g., for the non-yellow stickers on the
     * yellow face when presenting OLL algorithms.
     *
     * @return
     *     A map keyed by the color letter and containing each corresponding
     *     color value.
     */
    public static Map<Character, Integer> getColorLetterHashMap() {
        final Map<Character, Integer> map = new HashMap<>(7);

        map.put('W', FaceColor.UP.getColor());
        map.put('Y', FaceColor.DOWN.getColor());
        map.put('O', FaceColor.LEFT.getColor());
        map.put('R', FaceColor.RIGHT.getColor());
        map.put('G', FaceColor.FRONT.getColor());
        map.put('B', FaceColor.BACK.getColor());

        map.put('N', 0x9e9e9e); // Grey.

        return map;
    }

    public static Drawable getPllArrow(Context context, String name) {
        switch (name) {
            case "H":
                return getDrawable(context, R.drawable.pll_h_perm);
            case "Ua":
                return getDrawable(context, R.drawable.pll_ua_perm);
            case "Ub":
                return getDrawable(context, R.drawable.pll_ub_perm);
            case "Z":
                return getDrawable(context, R.drawable.pll_z_perm);
            case "Aa":
                return getDrawable(context, R.drawable.pll_aa_perm);
            case "Ab":
                return getDrawable(context, R.drawable.pll_ab_perm);
            case "E":
                return getDrawable(context, R.drawable.pll_e_perm);
            case "F":
                return getDrawable(context, R.drawable.pll_f_perm);
            case "Ga":
                return getDrawable(context, R.drawable.pll_ga_perm);
            case "Gb":
                return getDrawable(context, R.drawable.pll_gb_perm);
            case "Gc":
                return getDrawable(context, R.drawable.pll_gc_perm);
            case "Gd":
                return getDrawable(context, R.drawable.pll_gd_perm);
            case "Ja":
                return getDrawable(context, R.drawable.pll_ja_perm);
            case "Jb":
                return getDrawable(context, R.drawable.pll_jb_perm);
            case "Na":
                return getDrawable(context, R.drawable.pll_na_perm);
            case "Nb":
                return getDrawable(context, R.drawable.pll_nb_perm);
            case "Ra":
                return getDrawable(context, R.drawable.pll_ra_perm);
            case "Rb":
                return getDrawable(context, R.drawable.pll_rb_perm);
            case "T":
                return getDrawable(context, R.drawable.pll_t_perm);
            case "V":
                return getDrawable(context, R.drawable.pll_v_perm);
            case "Y":
                return getDrawable(context, R.drawable.pll_y_perm);
        }
        return null;
    }

    public static String getDefaultAlgs(String subset, String name) {
        switch (subset) {
            case "OLL":
                switch (name) {
                    case "OLL 01":
                        return "R U2 R2' F R F' U2 R' F R F' \n" +
                                "R U B' R B R2 U' R' F R F' \n" +
                                "y R U' R2 D' r U' r' D R2 U R' \n" +
                                "r U R' U R' r2 U' R' U R' r2 U2 r'";
                    case "OLL 02":
                        return "F R U R' U' F' f R U R' U' f' \n" +
                                "F R U R' U' S R U R' U' f' \n" +
                                "y r U r' U2 R U2 R' U2 r U' r' \n" +
                                "F R U r' U' R U R' M' U' F'";
                    case "OLL 03":
                        return "y' f R U R' U' f' U' F R U R' U' F' \n" +
                                "r' R2 U R' U r U2 r' U M' \n" +
                                "r' R U R' F2 R U L' U L M' \n" +
                                "y F U R U' R' F' U F R U R' U' F'";
                    case "OLL 04":
                        return "y' f R U R' U' f' U F R U R' U' F' \n" +
                                "M U' r U2 r' U' R U' R2 r \n" +
                                "y F U R U' R' F' U' F R U R' U' F' \n" +
                                "y2 r R2' U' R U' r' U2 r U' M";
                    case "OLL 05":
                        return "r' U2 R U R' U r \n" +
                                "y2 l' U2 L U L' U l \n" +
                                "y2 R' F2 r U r' F R \n" +
                                "L' U' L2 F' L' F2 U' F'";
                    case "OLL 06":
                        return "r U2 R' U' R U' r' \n" +
                                "y2 l U2 L' U' L U' l' \n" +
                                "y2 R U R2 F R F2 U F \n" +
                                "y' x' D R2 U' R' U R' D' x";
                    case "OLL 07":
                        return "r U R' U R U2 r' \n" +
                                "L' U2 L U2 L F' L' F \n" +
                                "F R' F' R U2 R U2 R' \n" +
                                "r U r' U R U' R' r U' r'";
                    case "OLL 08":
                        return "y2 r' U' R U' R' U2 r \n" +
                                "l' U' L U' L' U2 l \n" +
                                "R U2 R' U2 R' F R F' \n" +
                                "F' L F L' U2 L' U2 L";
                    case "OLL 09":
                        return "y R U R' U' R' F R2 U R' U' F' \n" +
                                "y2 R' U' R U' R' U R' F R F' U R \n" +
                                "r' R2 U2 R' U' R U' R' U' M' \n" +
                                "y' L' U' L U' L F' L' F L' U2 L";
                    case "OLL 10":
                        return "R U R' U R' F R F' R U2 R' \n" +
                                "R U R' y R' F R U' R' F' R \n" +
                                "y2 L' U' L U L F' L2 U' L U F \n" +
                                "R U R' y' r' U r U' r' U' r";
                    case "OLL 11":
                        return "r' R2 U R' U R U2 R' U M' \n" +
                                "M R U R' U R U2 R' U M' \n" +
                                "r U R' U R' F R F' R U2 r' \n" +
                                "y2 r U R' U R' F R F' R U2 r'";
                    case "OLL 12":
                        return "F R U R' U' F' U F R U R' U' F' \n" +
                                "y' r R2' U' R U' R' U2 R U' R r' \n" +
                                "y M U2 R' U' R U' R' U2 R U M' \n" +
                                "y M L' U' L U' L' U2 L U' M'";
                    case "OLL 13":
                        return "r U' r' U' r U r' F' U F \n" +
                                "F U R U' R2 F' R U R U' R' \n" +
                                "F U R U2 R' U' R U R' F' \n" +
                                "r U' r' U' r U r' y' R' U R";
                    case "OLL 14":
                        return "R' F R U R' F' R F U' F' \n" +
                                "R' F R U R' F' R y' R U' R' \n" +
                                "F' U' r' F r2 U r' U' r' F r \n" +
                                "r' U r U r' U' r y R U' R'";
                    case "OLL 15":
                        return "r' U' r R' U' R U r' U r \n" +
                                "y2 l' U' l L' U' L U l' U l \n" +
                                "r' U' M' U' R U r' U r \n" +
                                "y' R' U2 R U R' F U R U' R' F' R";
                    case "OLL 16":
                        return "r U r' R U R' U' r U' r' \n" +
                                "r U M U R' U' r U' r' \n" +
                                "y2 R' F R U R' U' F' R U' R' U2 R \n" +
                                "y2 l U l' L U L' U' l U' l'";
                    case "OLL 17":
                        return "R U R' U R' F R F' U2 R' F R F' \n" +
                                "f R U R' U' f' U' R U R' U' R' F R F' \n" +
                                "y2 F R' F' R2 r' U R U' R' U' M' \n" +
                                "y' F' r U r' U' S r' F r S'";
                    case "OLL 18":
                        return "r U R' U R U2 r2 U' R U' R' U2 r \n" +
                                "y R U2 R2 F R F' U2 M' U R U' r' \n" +
                                "y2 F R U R' d R' U2 R' F R F' \n" +
                                "y2 F R U R' U y' R' U2 R' F R F'";
                    case "OLL 19":
                        return "M U R U R' U' M' R' F R F' \n" +
                                "r' R U R U R' U' r R2' F R F' \n" +
                                "r' U2 R U R' U r2 U2 R' U' R U' r' \n" +
                                "R' U2 F R U R' U' F2 U2 F R";
                    case "OLL 20":
                        return "M U R U R' U' M2 U R U' r' \n" +
                                "r U R' U' M2 U R U' R' U' M' \n" +
                                "M' U M' U M' U M' U' M' U M' U M' U M' \n" +
                                "M' U' R' U' R U M2' U' R' U r";
                    case "OLL 21":
                        return "y R U2 R' U' R U R' U' R U' R' \n" +
                                "y F R U R' U' R U R' U' R U R' U' F' \n" +
                                "R U R' U R U' R' U R U2 R' \n" +
                                "R' U' R U' R' U R U' R' U2 R";
                    case "OLL 22":
                        return "R U2 R2 U' R2 U' R2 U2 R \n" +
                                "f R U R' U' f' F R U R' U' F' \n" +
                                "R' U2 R2 U R2 U R2 U2 R' \n" +
                                "R U2' R2' U' R2 U' R2' U2 R";
                    case "OLL 23":
                        return "R2 D R' U2 R D' R' U2 R' \n" +
                                "y2 R2 D' R U2 R' D R U2 R \n" +
                                "y R U R' U' R U' R' U2 R U' R' U2 R U R' \n" +
                                "R U R' U R U2 R2 U' R U' R' U2 R";
                    case "OLL 24":
                        return "r U R' U' r' F R F' \n" +
                                "y2 l' U' L U R U' r' F \n" +
                                "y' x' R U R' D R U' R' D' x \n" +
                                "r U R' U' L' U R U' x'";
                    case "OLL 25":
                        return "y F' r U R' U' r' F R \n" +
                                "R' F R B' R' F' R B \n" +
                                "F R' F' r U R U' r' \n" +
                                "y2 R U2 R' U' R U R' U' R U R' U' R U' R'";
                    case "OLL 26":
                        return "y R U2 R' U' R U' R' \n" +
                                "R' U' R U' R' U2 R \n" +
                                "y2 L' U' L U' L' U2 L \n" +
                                "R' U L U' R U L'";
                    case "OLL 27":
                        return "R U R' U R U2 R' \n" +
                                "y' R' U2 R U R' U R \n" +
                                "R U' L' U R' U' L \n" +
                                "y L' U2 L U L' U L";
                    case "OLL 28":
                        return "r U R' U' M U R U' R' \n" +
                                "y2 M' U M U2 M' U M \n" +
                                "M U M' U2 M U M' \n" +
                                "y' M' U' M U2 M' U' M";
                    case "OLL 29":
                        return "M U R U R' U' R' F R F' M' \n" +
                                "r2 D' r U r' D r2 U' r' U' r \n" +
                                "y R U R' U' R U' R' F' U' F R U R' \n" +
                                "y2 R' F R F' R U2 R' U' F' U' F";
                    case "OLL 30":
                        return "M U' L' U' L U L F' L' F M' \n" +
                                "y' r' D' r U' r' D r2 U' r' U r U r' \n" +
                                "y2 F R' F R2 U' R' U' R U R' F2 \n" +
                                "R2 U R' B' R U' R2 U R B R'";
                    case "OLL 31":
                        return "R' U' F U R U' R' F' R \n" +
                                "y2 S' L' U' L U L F' L' f \n" +
                                "y' F R' F' R U R U R' U' R U' R' \n" +
                                "y S R U R' U' f' U' F";
                    case "OLL 32":
                        return "S R U R' U' R' F R f' \n" +
                                "R U B' U' R' U R B R' \n" +
                                "y2 L U F' U' L' U L F L' \n" +
                                "R d L' d' R' U l U l'";
                    case "OLL 33":
                        return "R U R' U' R' F R F' \n" +
                                "F R U' R' U R U R' F' \n" +
                                "y2 L' U' L U L F' L' F \n" +
                                "y' r' U' r' D' r U r' D r2";
                    case "OLL 34":
                        return "y2 R U R' U' B' R' F R F' B \n" +
                                "y2 R U R2 U' R' F R U R U' F' \n" +
                                "F R U R' U' R' F' r U R U' r' \n" +
                                "y2 R U R' U' y' r' U' R U M'";
                    case "OLL 35":
                        return "R U2 R2' F R F' R U2 R' \n" +
                                "f R U R' U' f' R U R' U R U2 R' \n" +
                                "y' R U2 R' U' R U' R' U2 F R U R' U' F' \n" +
                                "R U2 R' U' y' r' U r U' r' U' r";
                    case "OLL 36":
                        return "y2 L' U' L U' L' U L U L F' L' F \n" +
                                "R' U' R U' R' U R U l U' R' U x \n" +
                                "R' U' R U' R' U R U R y R' F' R \n" +
                                "R U2 r D r' U2 r D' R' r'";
                    case "OLL 37":
                        return "F R U' R' U' R U R' F' \n" +
                                "F R' F' R U R U' R' \n" +
                                "R' F R F' U' F' U F \n" +
                                "y' R U2 R' F R' F' R2 U2 R'";
                    case "OLL 38":
                        return "R U R' U R U' R' U' R' F R F' \n" +
                                "L' U' L F L' U' L U L F' L' U L F' L' F";
                    case "OLL 39":
                        return "y L F' L' U' L U F U' L' \n" +
                                "y' R U R' F' U' F U R U2 R' \n" +
                                "y' R B' R' U' R U B U' R' \n" +
                                "R' r' D' r U' r' D r U R";
                    case "OLL 40":
                        return "y R' F R U R' U' F' U R \n" +
                                "R r D r' U r D' r' U' R' \n" +
                                "y' f R' F' R U R U' R' S' \n" +
                                "y' F R U R' U' F' R U R' U R U2 R'";
                    case "OLL 41":
                        return "y2 R U R' U R U2' R' F R U R' U' F' \n" +
                                "R U' R' U2 R U y R U' R' U' F' \n" +
                                "y' L F' L' F L F' L' F L' U' L U L' U' L \n" +
                                "f R U R' U' f' U' R U R' U R U2 R'";
                    case "OLL 42":
                        return "R' U' R U' R' U2 R F R U R' U' F' \n" +
                                "y R' F R F' R' F R F' R U R' U' R U R' \n" +
                                "L' U L U2 L' U' y' L' U L U F \n" +
                                "R' U R U2 R' U' F' U F U R";
                    case "OLL 43":
                        return "f' L' U' L U f \n" +
                                "y2 F' U' L' U L F \n" +
                                "y R' U' F' U F R \n" +
                                "y2 R' U' F R' F' R U R";
                    case "OLL 44":
                        return "f R U R' U' f' \n" +
                                "y2 F U R U' R' F' \n" +
                                "y2 r U x' R U' R' U x U' r' \n" +
                                "y' L d R U' R' F'";
                    case "OLL 45":
                        return "F R U R' U' F' \n" +
                                "y2 f U R U' R' f' \n" +
                                "y2 F' L' U' L U F \n" +
                                "F R2 D R' U R D' R2 U' F'";
                    case "OLL 46":
                        return "R' U' R' F R F' U R \n" +
                                "y F R U R' y' R' U R U2 R' \n" +
                                "y2 r' F' L' U L U' F r";
                    case "OLL 47":
                        return "F' L' U' L U L' U' L U F \n" +
                                "R' U' R' F R F' R' F R F' U R \n" +
                                "R' U' l' U R U' R' U R U' x' U R \n" +
                                "y2 B' R' U' R U R' U' R U B";
                    case "OLL 48":
                        return "F R U R' U' R U R' U' F' \n" +
                                "R U2 R' U' R U R' U2 R' F R F'";
                    case "OLL 49":
                        return "y2 r U' r2 U r2 U r2 U' r \n" +
                                "l U' l2 U l2 U l2 U' l \n" +
                                "R B' R2 F R2 B R2 F' R \n" +
                                "y2 R' F R' F' R2 U2 B' R B R'";
                    case "OLL 50":
                        return "r' U r2 U' r2' U' r2 U r' \n" +
                                "y2 R' F R2 B' R2 F' R2 B R' \n" +
                                "y' R U2 R' U' R U' R' F R U R' U' F' \n" +
                                "y2 l' U l2 U' l2 U' l2 U l'";
                    case "OLL 51":
                        return "f R U R' U' R U R' U' f' \n" +
                                "y2 F U R U' R' U R U' R' F' \n" +
                                "y' R' U' R' F R F' R U' R' U2 R \n" +
                                "y2 f' L' U' L U L' U' L U f";
                    case "OLL 52":
                        return "R U R' U R d' R U' R' F' \n" +
                                "R' U' R U' R' d R' U R B \n" +
                                "R' U' R U' R' U F' U F R \n" +
                                "R U R' U R U' y R U' R' F'";
                    case "OLL 53":
                        return "r' U' R U' R' U R U' R' U2 r \n" +
                                "y2 l' U' L U' L' U L U' L' U2 l \n" +
                                "y r' U2 R U R' U' R U R' U r \n" +
                                "y' l' U2 L U L' U' L U L' U l";
                    case "OLL 54":
                        return "r U R' U R U' R' U R U2 r' \n" +
                                "y' r U2 R' U' R U R' U' R U' r' \n" +
                                "F' L' U' L U L' U L U' L' U' L F \n" +
                                "y2 F R' F' R U2 F2 L F L' F";
                    case "OLL 55":
                        return "R U2 R2 U' R U' R' U2 F R F' \n" +
                                "y R' F R U R U' R2 F' R2 U' R' U R U R' \n" +
                                "r U2 R2 F R F' U2 r' F R F' \n" +
                                "R' U2 R2 U R' U R U2 y R' F' R";
                    case "OLL 56":
                        return "r U r' U R U' R' U R U' R' r U' r' \n" +
                                "F R U R' U' R F' r U R' U' r' \n" +
                                "y f R U R' U' f' F R U R' U' R U R' U' F' \n" +
                                "r' U' r U' R' U R U' R' U R r' U r";
                    case "OLL 57":
                        return "R U R' U' M' U R U' r' \n" +
                                "M' U M' U M' U2 M U M U M \n" +
                                "R U R' U' r R' U R U' r' \n" +
                                "M' U M' U M' U M' U2 M' U M' U M' U M'";
                }
                break;
            case "PLL":
                switch (name) {
                    case "Aa":
                        return "R' F R' B2 R F' R' B2 R2\n" +
                                "(x') R' D R' U2 R D' R' U2 R2 (x)\n" +
                                "(x) R' U R' D2 R U' R' D2 R2 (x')\n" +
                                "l' U R' D2 R U' R' D2 R2 (x')";
                    case "Ab":
                        return "R B' R F2 R' B R F2 R2\n" +
                                "(x) R D' R U2 R' D R U2 R2 (x')\n" +
                                "(x') R U' R D2 R' U R D2 R2 (x)\n" +
                                "(y' x) R2 D2 R U R' D2 R U' R (x')";
                    case "E":
                        return "y x' R U' R' D R U R' D' R U R' D R U' R' D' x\n" +
                                "R2 U R' U' y R U R' U' R U R' U' R U R' y' R U' R2\n" +
                                "z U2' R2' F R U R' U' R U R' U' R U R' U' F' R2 U2'\n" +
                                "y x' R U' R' D R U R' u2 R' U R D R' U' R x";
                    case "F":
                        return "R' U R U' R2 F' U' F U R F R' F' R2 U'\n" +
                                "R' U R U' R2 (y') R' U' R U (y x) R U R' U' R2 (x')\n" +
                                "R' U' F' R U R' U' R' F R2 U' R' U' R U R' U R\n" +
                                "(y’) L U F L' U' L U L F' L2 U L U L' U' L U' L'";
                    case "Ga":
                        return "R2 U (R' U R' U') R U' R2 D U' R' U R D'\n" +
                                "(y) R2' u R' U R' U' R u' R2 (y') R' U R\n" +
                                "(y) R2 U R' U R' U' R U' R2 D U' R' U R D'\n" +
                                "(y2) F2' D R' U R' U' R D' F2 L' U L";
                    case "Gb":
                        return "R' U' R y R2 u R' U R U' R u' R2\n" +
                                "R' U' R U D' R2 U R' U R U' R U' R2 D\n" +
                                "y F' U' F R2 u R' U R U' R u' R2\n" +
                                "R' d' F R2 u R' U R U' R u' R2";
                    case "Gc":
                        return "(y) R2' u' R U' R U R' u R2 (y) R U' R'\n" +
                                "(y) R2' u' R U' R U R' u R2 B U' B'\n" +
                                "(y) R2' U' R U' R U R' U R2 D' U R U' R' D\n" +
                                "(y) R2' D' F U' F U F' D R2 B U' B'";
                    case "Gd":
                        return "(y2) R U R' (y') R2 u' R U' R' U R' u R2\n" +
                                "(y2) R U R' F2 D' L U' L' U L' D F2\n" +
                                "(y2) L U2 L' U F' L' U' L U L F U L' U' L' U L	\n" +
                                "(y2) l2 U' L2 U' F2 L' U' R U2 L' U l (x')";
                    case "H":
                        return "M2 U M2 U2 M2 U M2\n" +
                                "M2 U' M2 U2 M2 U' M2\n" +
                                "R2 U2 R U2 R2 U2 R2 U2 R U2 R2\n" +
                                "M2' U' M2' U2' M2' U' M2'";
                    case "Ja":
                        return "B' U F' U2 B U' B' U2 F B U'\n" +
                                "(y) R' U L' U2 R U' R' U2 R L\n" +
                                "(y') L' U R' U2 L U' L' U2 R L\n" +
                                "(y') L' U R' (z) R2 U R' U' R2 U D (z')";
                    case "Jb":
                        return "R U R' F' R U R' U' R' F R2 U' R' U'\n" +
                                "R U2 R' U' R U2 L' U R' U' r x\n" +
                                "R U2 R' U' R U2 L' U R' U' L\n" +
                                "L' U R U' L U2' R' U R U2' R'";
                    case "Na":
                        return "R U R' U R U R' F' R U R' U' R' F R2 U' R' U2 R U' R'\n" +
                                "L U' R U2 L' U R' L U' R U2 L' U R'\n" +
                                "z U R' D R2 U' R D' U R' D R2 U' R D' z'\n" +
                                "r' D r U2 r' D r U2 r' D r U2 r' D r U2 r' D r";
                    case "Nb":
                        return "R' U L' U2 R U' L R' U L' U2 R U' L\n" +
                                "R' U R U' R' F' U' F R U R' F R' F' R U' R\n" +
                                "z D' R U' R2 D R' U D' R U' R2 D R' U z'\n" +
                                "z U' R D' R2 U R' D U' R D' R2 U R' D z'";
                    case "Ra":
                        return "(y2) L U2 L' U2 L F' L' U' L U L F L2\n" +
                                "(y') R U R' F' R U2 R' U2 R' F R U R U2 R'\n" +
                                "(y') R U' R' U' R U R D R' U' R D' R' U2 R'\n" +
                                "R U2 R' U2 R B' R' U' R U R B R2 U";
                    case "Rb":
                        return "R' U2 R U2 R' F R U R' U' R' F' R2\n" +
                                "R' U2 R' D' R U' R' D R U R U' R' U' R\n" +
                                "y R2 F R U R U' R' F' R U2 R' U2 R\n" +
                                "y' R U2' R' U2 R' F R2 U' R' U' R U R' F' R U R' U R U2 R'";
                    case "T":
                        return "R U R' U' R' F R2 U' R' U' R U R' F'\n" +
                                "R U R' U' R' F R2 U' R' U F' L' U L\n" +
                                "R2 U R2 U' R2 U' D R2 U' R2 U R2 D'\n" +
                                "y F2 D R2 U' R2 F2 D' L2 U L2 U'";
                    case "Ua":
                        return "R2 U' R' U' R U R U R U' R\n" +
                                "y2 R U' R U R U R U' R' U' R2\n" +
                                "M2 U M' U2 M U M2\n" +
                                "y2 M2 U M U2 M' U M2";
                    case "Ub":
                        return "R' U R' U' R' U' R' U R U R2\n" +
                                "y2 M2 U' M U2 M' U' M2\n" +
                                "y2 R2' U R U R' U' R' U' R' U R'\n" +
                                "M2 U' M' U2 M U' M2";
                    case "V":
                        return "R' U R' d' R' F' R2 U' R' U R' F R F\n" +
                                "R' U R' U' y R' F' R2 U' R' U R' F R F\n" +
                                "z D' R2 D R2' U R' D' R U' R U R' D R U'\n" +
                                "R U2 R' D R U' R U' R U R2 D R' U' R D2";
                    case "Y":
                        return "F R U' R' U' R U R' F' R U R' U' R' F R F'\n" +
                                "F R' F R2 U' R' U' R U R' F' R U R' U' F'\n" +
                                "R2 U' R2 U' R2 U y' R U R' B2 R U' R'\n" +
                                "R2 U' R' U R U' y' x' L' U' R U' R' U' L U";
                    case "Z":
                        return "M2 U M2 U M' U2 M2 U2 M'\n" +
                                "y M2' U' M2' U' M' U2' M2' U2' M'\n" +
                                "M' U' M2' U' M2' U' M' U2' M2'\n" +
                                "R' U' R2 U R U R' U' R U R U' R U' R'";
                }
        }
        return "";
    }
}
