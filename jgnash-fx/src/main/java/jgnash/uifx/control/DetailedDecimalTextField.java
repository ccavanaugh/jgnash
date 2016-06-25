package jgnash.uifx.control;

import java.math.BigDecimal;
import java.util.Objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Skin;

import jgnash.uifx.control.skin.DetailedDecimalTextFieldSkin;
import jgnash.util.NotNull;

/**
 * A {@code DecimalTextField} that supports use of a popup or dialog by overriding
 * {@code show()} and {@code hide()}.
 *
 * @author Craig Cavanaugh
 */
public class DetailedDecimalTextField extends ComboBoxBase<BigDecimal> {

    private static final String STYLE_CLASS_COMBO_BOX = "combo-box"; //NON-NLS
    private static final String STYLE_CLASS_FORMATTED_COMBO_BOX = "date-picker"; //NON-NLS
    private static final String STYLE_CLASS_DECIMAL_COMBO_BOX = "decimal-details"; //NON-NLS

    public DetailedDecimalTextField() {
        getStyleClass().addAll(STYLE_CLASS_COMBO_BOX, STYLE_CLASS_FORMATTED_COMBO_BOX, STYLE_CLASS_DECIMAL_COMBO_BOX);
    }

    /**
     * The editor for the ComboBox. It is used for both editable text field and non-editable text field.
     */
    private ReadOnlyObjectWrapper<DecimalTextField> editor;

    public final DecimalTextField getEditor() {
        return editorProperty().get();
    }

    private ReadOnlyObjectProperty<DecimalTextField> editorProperty() {
        if (editor == null) {
            editor = new ReadOnlyObjectWrapper<>(this, "editor"); //NON-NLS
            DecimalTextField field = createDecimalTextField();
            field.decimalProperty().bindBidirectional(valueProperty());
            field.editableProperty().bindBidirectional(editableProperty());
            editor.set(field);
        }
        return editor.getReadOnlyProperty();
    }

    /**
     * Decimal property.
     *
     * @see DecimalTextField#decimalProperty()
     * @return BigDecimal object property
     */
    public ObjectProperty<BigDecimal> decimalProperty() {
        return getEditor().decimalProperty();
    }

    /**
     * Gets the decimal value.
     *
     * @see DecimalTextField#getDecimal()
     *
     * @return BigDecimal value
     */
    public @NotNull
    BigDecimal getDecimal() {
        return getEditor().getDecimal();
    }

    /**
     * Sets the value for the field.
     *
     * @see DecimalTextField#setDecimal(BigDecimal)
     * @param decimal BigDecimal value to display.  May not be null
     */
    protected void setDecimal(@NotNull final BigDecimal decimal) {
        Objects.requireNonNull(decimal);

        getEditor().setDecimal(decimal);
    }

    /**
     * Creates a DecimalTextField. Subclass can override it to create a DecimalTextField subclass.
     *
     * @return a DecimalTextField
     */
    private DecimalTextField createDecimalTextField() {
        return new DecimalTextField();
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new DetailedDecimalTextFieldSkin(this);
    }
}
