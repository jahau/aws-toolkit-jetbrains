// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.schemas.search

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import icons.AwsIcons
import software.aws.toolkits.jetbrains.core.explorer.actions.SingleResourceNodeAction
import software.aws.toolkits.jetbrains.services.schemas.SchemaRegistryNode
import software.aws.toolkits.jetbrains.services.telemetry.TelemetryService

class SearchSchemasAction : AnAction("Search Schemas", null, AwsIcons.Actions.SCHEMA_SEARCH) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getRequiredData(PlatformDataKeys.PROJECT)

        val dialog = SchemaSearchDialogManager.INSTANCE.searchAllRegistriesDialog(project)
        dialog.show()

        TelemetryService.recordSimpleTelemetry(project, "schemas_search")
    }
}

class SearchSchemasInRegistryAction :
    SingleResourceNodeAction<SchemaRegistryNode>("Search Schemas in Registry", null, AwsIcons.Actions.SCHEMA_SEARCH) {
    override fun actionPerformed(selected: SchemaRegistryNode, e: AnActionEvent) {
        val project = e.getRequiredData(PlatformDataKeys.PROJECT)
        val registry = selected.value.registryName()

        val dialog = SchemaSearchDialogManager.INSTANCE.searchRegistryDialog(registry, project)
        dialog.show()
    }
}
