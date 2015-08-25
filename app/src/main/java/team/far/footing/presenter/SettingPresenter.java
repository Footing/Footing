package team.far.footing.presenter;

import team.far.footing.model.IFileModel;
import team.far.footing.model.impl.FileModel;
import team.far.footing.ui.vu.ISettingVu;

/**
 * Created by luoyy on 2015/8/25 0025.
 */
public class SettingPresenter {
    private ISettingVu settingVu;
    private IFileModel fileModel;

    public SettingPresenter(ISettingVu settingVu) {
        this.settingVu = settingVu;
        fileModel = FileModel.getInstance();
    }

    public void cachesize() {
        String cachesize = fileModel.getCacheFormatSize();
        if (settingVu != null) settingVu.showCache(cachesize);
    }

    public void cleancache() {
        fileModel.clearCache();

    }
}
