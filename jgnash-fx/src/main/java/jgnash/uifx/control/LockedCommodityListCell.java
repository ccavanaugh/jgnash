package jgnash.uifx.control;

import javafx.scene.control.ListCell;

import jgnash.engine.CommodityNode;
import jgnash.uifx.skin.StyleClass;
import jgnash.util.LockedCommodityNode;

/**
 * Provides visual feedback that items are locked and may not be moved.
 *
 * @author Craig Cavanaugh
 */
public class LockedCommodityListCell<T extends CommodityNode> extends ListCell<LockedCommodityNode<T>> {

    @Override
    public void updateItem(final LockedCommodityNode<T> item, final boolean empty) {
        super.updateItem(item, empty);  // required

        if (!empty) {
            if (item.isLocked()) {
                setId(StyleClass.DISABLED_CELL_ID);
                setDisable(true);
            } else {
                setId(StyleClass.ENABLED_CELL_ID);
                setDisable(false);
            }

            setText(item.toString());
        } else {
            setText("");
        }
    }
}
