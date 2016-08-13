package me.Fupery.ArtMap.Menu.HelpMenu;

import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.Menu.API.ChildMenu;
import me.Fupery.ArtMap.Menu.API.MenuTemplate;
import me.Fupery.ArtMap.Menu.API.StoragePattern;
import me.Fupery.ArtMap.Menu.Button.Button;
import me.Fupery.ArtMap.Menu.Button.CloseButton;
import me.Fupery.ArtMap.Menu.Button.StaticButton;
import me.Fupery.ArtMap.Menu.Templates.BasicMenu;
import me.Fupery.ArtMap.Utils.ArtDye;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

public class DyeMenu extends BasicMenu implements ChildMenu {
    private final MenuTemplate parent;

    public DyeMenu(MenuTemplate parent) {
        super(ArtMap.getLang().getMsg("MENU_DYES"), InventoryType.CHEST, StoragePattern.CACHED_WEAKLY);
        this.parent = parent;
    }

    @Override
    public Button[] getButtons(Player viewer) {
        Button[] buttons = new Button[27];
        ArtDye[] dyes = ArtDye.values();
        buttons[0] = new StaticButton(Material.SIGN, ArtMap.getLang().getArray("INFO_DYES"));
        buttons[26] = new CloseButton();

        for (int i = 1; i < 26; i++) {
            ArtDye dye = dyes[i - 1];
            Material displayMaterial = dye.getRecipeItem().getItemType();
            String displayName = dye.getDisplay() + dye.name().toLowerCase();
            buttons[i] = new StaticButton(displayMaterial, "", new String[]{displayName});
            buttons[i].setDurability(dye.getRecipeItem().getData());
        }
        return buttons;
    }

    @Override
    public MenuTemplate getParent() {
        return parent;
    }
}
