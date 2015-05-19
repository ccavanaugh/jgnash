package jgnash.uifx.control;

import java.math.BigDecimal;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Skin;

import jgnash.uifx.control.skin.DetailedDecimalTextFieldSkin2;
import jgnash.util.NotNull;

/**
 * A {@code DecimalTextField} that supports use of a popup or dialog by overriding
 * {@code show()} and {@code hide()}
 *
 * @author Craig Cavanaugh
 */
public class DetailedDecimalTextField2 extends ComboBoxBase<BigDecimal> {

    private static final String STYLE_CLASS_COMBO_BOX = "combo-box"; //NON-NLS
    private static final String STYLE_CLASS_FORMATTED_COMBO_BOX = "date-picker"; //NON-NLS

    public DetailedDecimalTextField2() {
        getStyleClass().addAll(STYLE_CLASS_COMBO_BOX, STYLE_CLASS_FORMATTED_COMBO_BOX);
    }

    /**
     * The editor for the ComboBox. It is used for both editable combobox and non-editable combobox.
     */
    private ReadOnlyObjectWrapper<DecimalTextField> editor;

    public final DecimalTextField getEditor() {
        return editorProperty().get();
    }

    public final ReadOnlyObjectProperty<DecimalTextField> editorProperty() {
        if (editor == null) {
            editor = new ReadOnlyObjectWrapper<>(this, "editor"); //NON-NLS
            DecimalTextField field = createFormattedTextField();
            field.decimalProperty().bindBidirectional(valueProperty());
            field.editableProperty().bindBidirectional(editableProperty());
            editor.set(field);
        }
        return editor.getReadOnlyProperty();
    }

    /**
     * @see DecimalTextField#decimalProperty()
     */
    public ObjectProperty<BigDecimal> decimalProperty() {
        return getEditor().decimalProperty();
    }

    /**
     * @see DecimalTextField#getDecimal()
     */
    public @NotNull
    BigDecimal getDecimal() {
        return getEditor().getDecimal();
    }

    /**
     * @see DecimalTextField#setDecimal(BigDecimal)
     */
    public void setDecimal(@NotNull final BigDecimal decimal) {
        getEditor().setDecimal(decimal);
    }

    /**
     * Creates a DecimalTextField. Subclass can override it to create a DecimalTextField subclass.
     *
     * @return a DecimalTextField
     */
    protected DecimalTextField createFormattedTextField() {
        return new DecimalTextField();
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new DetailedDecimalTextFieldSkin2(this);
    }
}
