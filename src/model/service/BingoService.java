package model.service;

import java.util.List;
import model.dao.UserBingoDAO;

public final class BingoService {
    private final UserBingoDAO repository=new UserBingoDAO();
    public List<UserBingoDAO.BingoEntry> load(int user){if(user<=0)throw new IllegalArgumentException("Sesión no válida.");return repository.loadByUser(user);}
    public void save(int user,int slot,String text,boolean completed){if(user<=0||slot<0||slot>8||text!=null&&text.length()>255)throw new IllegalArgumentException("Predicción no válida (máximo 255 caracteres).");repository.upsert(user,slot,text,completed);}
}
