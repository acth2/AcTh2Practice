package fr.acth2.practice.utils;

import fr.acth2.practice.gameplay.Arena;
import net.minecraft.core.BlockPos;

import java.util.List;

public class References {
    public static final String MODID = "acth2practice";
    public static final String NAME = "AcTh2Practice";
    public static final String VERSION = "4.2.5";

    //  CHANGEZ PAR VOS VALEURS ICI:

    public static final BlockPos SPAWN_POS = new BlockPos(0, 101, 0);

    /* Construction exemple d'une arene:                   POSITION DU JOUEUR 1   POSITION DU JOUEUR 2
    * public static Arena EXEMPLE_ARENA = new Arena("NOM", new BlockPos(0, 0, 0), new BlockPos(0, 0, 25));
    * VOUS DEVEZ ENSUITE ENREGISTRER L'ARENE DANS LE CONSTRUCTEUR DE LA CLASSE PRINCIPAL! (vous pouvez simplement recherché le commentaire "// LES ARENES A ENREGISTRER" et vous allez trouvé l'endroit)
    */

    public static Arena kh3sa = new Arena("kh3sa", new BlockPos(2140, 101, 2103), new BlockPos(2089, 101, 2103));
    public static Arena blue0 = new Arena("blue0", new BlockPos(1063, 101, 1025), new BlockPos(985, 101, 1025));

    // LISTE DES MOTS INTERDITS
    public static List<String> BANNED_WORDS = List.of("loser", "idiot", "dumbass", "kys", "fuck you", "nigger", "nigga", "fdp", "tg", "stfu", "ntm", "negro");

    public static String SERVER_NAME = "AcTh2Practice";
}
