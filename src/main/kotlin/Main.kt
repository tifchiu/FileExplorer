import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.stage.Stage
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files

class Main : Application()  {

    private var showHiddenFiles = false
    private var dir = File("${System.getProperty("user.dir")}/test/")
    private val home = File("${System.getProperty("user.dir")}/test/")

    private fun doubleClickEventHandler(tree: ListView<String>, layout: BorderPane) {
        val next = tree.selectionModel.selectedItem
        val newdir = File("${dir}\\${next}")
        if (newdir.listFiles() == null) {
            openFile(newdir, layout)
        } else {
            openDir(newdir, tree,layout)
        }
    }

    private fun goToParent(tree: ListView<String>, layout: BorderPane) {
        val parentDir = dir.parentFile
        openDir(parentDir,tree,layout)
        dir = parentDir
    }

    private fun updateStatusBar(tree: ListView<String>, layout: BorderPane) {
        val next = tree.selectionModel.selectedItem
        val newdir = File("${dir}\\${next}")
        layout.bottom = Label(newdir.toString())
    }

    private fun renameEventHandler(tree: ListView<String>, layout: BorderPane) {
        val file = tree.selectionModel.selectedItem
        val path = File("${dir}\\${file}")

        val dialog = TextInputDialog()
        dialog.headerText = "What would you like to rename $file to?"
        val rename = dialog.showAndWait()

        if (rename.isPresent) {
            val newFile = File("${dir}\\${rename.get()}")
            val success = path.renameTo(newFile)
            if (!success) {
                val alert = Alert(Alert.AlertType.ERROR, "Could not rename $file to ${rename.get()}")
                alert.show()
            } else {
                val index = tree.selectionModel.selectedIndex
                tree.items[index] = rename.get()
                layout.bottom = Label(newFile.toString())
            }
        }
    }

    private fun moveEventHandler(tree: ListView<String>, layout: BorderPane) {
        val file = tree.selectionModel.selectedItem
        val path = File("${dir}\\${file}")

        val dialog = TextInputDialog()
        dialog.headerText = "Which directory would you like to move $file to?"
        val rename = dialog.showAndWait()

        if (rename.isPresent) {
            val newFile = File("${dir}\\${rename.get()}\\${file}")
            val success = path.renameTo(newFile)
            if (!success) {
                val alert = Alert(Alert.AlertType.ERROR, "Could not move $file to ${rename.get()}.")
                alert.show()
            } else {
                tree.items.remove(file)
                layout.bottom = Label(dir.toString())
                layout.center = null
            }
        }
    }

    private fun deleteEventHandler(tree: ListView<String>, layout: BorderPane) {
        val file = tree.selectionModel.selectedItem
        val alert = Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete ${file}?", ButtonType.YES, ButtonType.CANCEL)
        val result = alert.showAndWait()
        if (result.get() == ButtonType.YES) {
            val path = File("${dir}\\${file}")
            val success = path.delete()
            if (success) {
                val index = tree.selectionModel.selectedIndex
                if (file == tree.items[index]) {
                    layout.center = null
                }
                tree.items.removeAt(index)
                layout.bottom = Label(dir.parentFile.path)
            } else {
                val error = Alert(Alert.AlertType.ERROR, "Could not delete $file.")
                error.show()
            }
        }
    }

    private fun openDir(newdir: File, tree: ListView<String>,layout: BorderPane) {
        if (Files.isReadable(newdir.toPath())) {
            layout.center = null
            tree.items.clear()
            newdir.listFiles().forEach {
                if (it.name[0] != '.' || showHiddenFiles) {
                    tree.items.add(it.name)
                }
            }
            dir = newdir
            layout.bottom = Label(dir.path)
        } else {
            val alert = Alert(Alert.AlertType.ERROR, "${newdir.name} is unreadable.")
            alert.show()
        }
    }

    private fun openFile(newdir: File, layout: BorderPane)  {
        if (Files.isReadable(newdir.toPath())) {
            val fileType = newdir.extension
            if (fileType in listOf("png", "jpg", "bmp")) {
                var image = Image(FileInputStream(newdir))
                val imageView = ImageView(image)
                imageView.isPreserveRatio = true
                val stackPane = StackPane(imageView)
                stackPane.setMinSize(0.0, 0.0)
                stackPane.alignment = Pos.CENTER
                imageView.fitWidthProperty().bind(stackPane.widthProperty())
                imageView.fitHeightProperty().bind(stackPane.heightProperty())
                layout.center = stackPane
            } else if (fileType in listOf("txt", "md")) {
                val text = Text(newdir.readText())
                val scrollPane = ScrollPane()
                scrollPane.isFitToWidth = true
                scrollPane.content = text
                text.wrappingWidthProperty().bind(scrollPane.widthProperty())
                layout.center = scrollPane
            } else {
                val alert = Alert(Alert.AlertType.ERROR, "Cannot open ${newdir.name} due to unrecognized file type.")
                alert.show()
            }
        } else {
            val alert = Alert(Alert.AlertType.ERROR, "${newdir.name} is unreadable.")
            alert.show()
        }
    }

    private fun setMenuBar(tree : ListView<String>, layout: BorderPane) : MenuBar {
        val menuBar = MenuBar()
        val fileMenu = Menu("File")
        val viewMenu = Menu("View")
        val actionsMenu = Menu("Actions")
        val optionsMenu = Menu("Options")
        menuBar.menus.add(fileMenu)
        menuBar.menus.add(viewMenu)
        menuBar.menus.add(actionsMenu)
        menuBar.menus.add(optionsMenu)

        val toggleHidden = MenuItem("Toggle Hidden Files")
        val rename = MenuItem("Rename")
        val delete = MenuItem("Delete")
        val move = MenuItem("Move")

        toggleHidden.setOnAction {
            if (showHiddenFiles == false) {
                showHiddenFiles = true;
                dir.listFiles().forEach {
                    if (it.name[0] == '.') tree.items.add(it.name)
                }
            } else {
                showHiddenFiles = false
                tree.items.forEach() {
                    if (it[0] == '.') {
                        tree.items.remove(it)
                    }
                }
            }
        }

        rename.setOnAction {
            renameEventHandler(tree,layout)
        }

        delete.setOnAction {
            deleteEventHandler(tree,layout)
        }

        move.setOnAction {
            moveEventHandler(tree,layout)
        }

        optionsMenu.items.add(toggleHidden)
        actionsMenu.items.add(rename)
        actionsMenu.items.add(delete)
        actionsMenu.items.add(move)

        return menuBar
    }

    private fun setToolBar(tree: ListView<String>, layout: BorderPane,stage: Stage) : ToolBar {
        val toolBar = ToolBar()
        val homeButton = Button("Home")
        toolBar.items.add(homeButton)
        val backButton = Button("Back")
        toolBar.items.add(backButton)
        val nextButton = Button("Next")
        toolBar.items.add(nextButton)
        val deleteButton = Button("Delete")
        toolBar.items.add(deleteButton)
        val renameButton = Button("Rename")
        toolBar.items.add(renameButton)
        val moveButton = Button("Move")
        toolBar.items.add(moveButton)

        backButton.setOnMouseClicked {
            openDir(dir.parentFile,tree,layout)
        }
        homeButton.setOnMouseClicked {
            openDir(home,tree,layout)
        }
        nextButton.setOnMouseClicked {
            doubleClickEventHandler(tree,layout)
        }
        renameButton.setOnMouseClicked {
            renameEventHandler(tree,layout)
        }
        moveButton.setOnMouseClicked {
            moveEventHandler(tree,layout)
        }
        deleteButton.setOnMouseClicked {
            deleteEventHandler(tree,layout)
        }

        return toolBar
    }

    override fun start(stage: Stage) {

        // create the root of the scene graph
        // BorderPane supports placing children in regions around the screen
        val layout = BorderPane()

        // left: tree
        val tree = ListView<String>()
        dir.listFiles().forEach {
            if (it.name[0] !=  '.' || showHiddenFiles) {
                tree.items.add(it.name)
            }
        }

        // handle mouse clicked action
        tree.setOnMouseClicked { event ->
            when (event.clickCount) {
                1 -> updateStatusBar(tree, layout)
                2 -> doubleClickEventHandler(tree, layout)
            }
        }

        tree.setOnKeyPressed { key ->
            when (key.code) {
                KeyCode.ENTER      -> doubleClickEventHandler(tree,layout)
                KeyCode.BACK_SPACE -> goToParent(tree,layout)
                KeyCode.DELETE     -> goToParent(tree,layout)
                KeyCode.DOWN       -> updateStatusBar(tree,layout)
                KeyCode.UP         -> updateStatusBar(tree,layout)
            }
        }

        val toolBar = setToolBar(tree, layout,stage)
        val menuBar = setMenuBar(tree, layout)

        var top = VBox()
        top.children.add(menuBar)
        top.children.add(toolBar)

        val curr = Label(dir.toString())

        // build the scene graph
        layout.top = top
        layout.left = tree
        layout.bottom = curr

        // create and show the scene
        val scene = Scene(layout)
        stage.title = "File Browser"
        stage.width = 800.0
        stage.height = 500.0
        stage.scene = scene
        stage.show()
    }
}