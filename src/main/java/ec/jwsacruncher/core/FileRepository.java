/*
 * Copyright 2013 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package ec.jwsacruncher.core;

import ec.demetra.workspace.WorkspaceFamily;
import ec.demetra.workspace.WorkspaceItem;
import ec.demetra.workspace.file.FileWorkspace;
import ec.tss.sa.SaProcessing;
import ec.tstoolkit.algorithm.ProcessingContext;
import ec.tstoolkit.timeseries.calendars.GregorianCalendarManager;
import ec.tstoolkit.timeseries.calendars.IGregorianCalendarProvider;
import ec.tstoolkit.timeseries.regression.TsVariables;
import ec.tstoolkit.utilities.NameManager;
import ec.tstoolkit.utilities.Paths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Jean Palate
 */
@lombok.experimental.UtilityClass
public class FileRepository {

    public void storeSaProcessing(FileWorkspace ws, WorkspaceItem item, SaProcessing processing) throws IOException {
        makeSaProcessingBackup(ws, item);
        ws.store(item, processing);
    }

    public Map<WorkspaceItem, SaProcessing> loadAllSaProcessing(FileWorkspace ws, ProcessingContext context) throws IOException {
        Map<WorkspaceItem, SaProcessing> result = new LinkedHashMap<>();
        for (WorkspaceItem item : ws.getItems()) {
            WorkspaceFamily family = item.getFamily();
            if (family.equals(WorkspaceFamily.UTIL_CAL)) {
                applyCalendars(context, (GregorianCalendarManager) ws.load(item));
            } else if (family.equals(WorkspaceFamily.UTIL_VAR)) {
                applyVariables(context, item.getId(), (TsVariables) ws.load(item));
            } else if (family.equals(WorkspaceFamily.SA_MULTI)) {
                result.put(item, (SaProcessing) ws.load(item));
            }
        }
        return result;
    }

    private void makeSaProcessingBackup(FileWorkspace ws, WorkspaceItem item) throws IOException {
        Path source = ws.getFile(item);
        Path target = source.getParent().resolve(Paths.changeExtension(source.getFileName().toString(), "bak"));
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void applyVariables(ProcessingContext context, String id, TsVariables value) {
        NameManager<TsVariables> manager = context.getTsVariableManagers();
        manager.set(id, value);
        manager.resetDirty();
    }

    private void applyCalendars(ProcessingContext context, GregorianCalendarManager value) {
        GregorianCalendarManager manager = context.getGregorianCalendars();
        for (String s : value.getNames()) {
            if (!manager.contains(s)) {
                IGregorianCalendarProvider cal = value.get(s);
                manager.set(s, cal);
            }
        }
        manager.resetDirty();
    }
}
