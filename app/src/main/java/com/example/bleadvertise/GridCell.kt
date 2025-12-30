import com.example.bleadvertise.ButtonConfig

sealed class GridCell {
    data object Empty : GridCell()
    data class ButtonCell(val config: ButtonConfig) : GridCell()
}