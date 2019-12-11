// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
package software.aws.toolkits.jetbrains.services.s3.bucketEditor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.treeStructure.treetable.TreeTable
import software.amazon.awssdk.services.s3.S3Client
import software.aws.toolkits.core.utils.getLogger
import software.aws.toolkits.jetbrains.services.s3.S3VirtualBucket
import software.aws.toolkits.jetbrains.services.s3.objectActions.UploadObjectAction
import software.aws.toolkits.jetbrains.utils.notifyError
import software.aws.toolkits.resources.message
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.tree.DefaultMutableTreeNode

class S3TreeTable(
    private val treeTableModel: S3TreeTableModel,
    private val bucketVirtual: S3VirtualBucket,
    private val project: Project,
    private val s3Client: S3Client
) : TreeTable(treeTableModel) {

    private val dropTargetListener = object : DropTargetAdapter() {
        override fun drop(dropEvent: DropTargetDropEvent) {
            val row = rowAtPoint(dropEvent.location).takeIf { it >= 0 } ?: return
            val node = getNodeForRow(row) ?: return
            val data = try {
                dropEvent.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE)
                dropEvent.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
            } catch (e: UnsupportedFlavorException) {
                // When the drag and drop data is not what we expect (like when it is text) this is thrown and can be safey ignored
                LOG.info("Unsupported flavor attempted to be dragged and dropped", e)
                return
            }

            val lfs = LocalFileSystem.getInstance()
            val virtualFiles = data.mapNotNull {
                lfs.findFileByIoFile(it)
            }

            ApplicationManager.getApplication().executeOnPooledThread {
                val action = UploadObjectAction(bucketVirtual, this@S3TreeTable)
                virtualFiles.forEach {
                    try {
                        action.uploadObjectAction(s3Client, project, it, node)
                        invalidateLevel(node)
                        refresh()
                    } catch (e: Exception) {
                        e.notifyError(message("s3.upload.object.failed"))
                    }
                }
            }
        }
    }

    private val mouseListener = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            val row = rowAtPoint(e.point).takeIf { it >= 0 } ?: return
            handleLoadingMore(row, e)
        }
    }

    private fun handleLoadingMore(row: Int, e: MouseEvent) {
        if (e.clickCount != 2) {
            return
        }
        val continuationNode = (tree.getPathForRow(row).lastPathComponent as? DefaultMutableTreeNode)?.userObject as? S3TreeContinuationNode ?: return
        val parent = continuationNode.parent ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            parent.loadMore(continuationNode.token)
            refresh()
        }
    }

    init {
        // Associate the drop target listener with this instance which will allow uploading by drag and drop
        DropTarget(this, dropTargetListener)
    }

    fun refresh() {
        runInEdt {
            clearSelection()
            treeTableModel.structureTreeModel.invalidate()
        }
    }

    init {
        super.addMouseListener(mouseListener)
    }

    fun getNodeForRow(row: Int): S3TreeNode? {
        val path = tree.getPathForRow(convertRowIndexToModel(row))
        return (path.lastPathComponent as DefaultMutableTreeNode).userObject as? S3TreeNode
    }

    fun getSelectedNodes(): List<S3TreeNode> = selectedRows.map { getNodeForRow(it) }.filterNotNull()

    fun removeRows(rows: List<Int>) =
        runInEdt {
            rows.map {
                val path = tree.getPathForRow(it)
                path.lastPathComponent as DefaultMutableTreeNode
            }.forEach {
                val userNode = it.userObject as? S3TreeNode ?: return@forEach
                ((it.parent as? DefaultMutableTreeNode)?.userObject as? S3TreeDirectoryNode)?.removeChild(userNode)
            }
        }

    fun invalidateLevel(node: S3TreeNode) {
        node.parent?.removeAllChildren()
    }

    companion object {
        private val LOG = getLogger<S3TreeTable>()
    }
}
