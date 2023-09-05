package br.ufrn.raszz.persistence;

import java.util.List;

import br.ufrn.raszz.model.RefCaller;
import br.ufrn.raszz.model.RefElement;
import br.ufrn.raszz.model.RefacToolType;

public abstract class RefacDAO extends AbstractDAO {

	/*public abstract List<Object> getSampledRevisionWithBugsWithBICIdentifiedByProject(String project);
	
	public abstract List<Object> getNotSampledRevisionWithBugsWithBICIdentifiedByProject(String project);
	
	public abstract List<Object> getAllRevisionWithBugsWithBICIdentifiedByProject(String project);
	
	public abstract List<Object> getFixRevisionWithBugsWithBICIdentifiedByProject(String project);
	
	public abstract List<Object> getFixRevisionWithBugsWithoutBICIdentifiedByProject(String project);
	
	public abstract List<Object> getFixRevisionByProject(String project);*/
	
	public abstract void saveRefDiffResults(List<RefElement> refElements);
	
	public abstract void saveCallersRefDiffResults(List<RefCaller> callers);
	
	public abstract void saveRefacResults(RefElement ref, String revisionType);

	public abstract void saveCallersRefDiffResults(RefCaller caller, String revisionType);
	
	public abstract void insertRefacRevisionsProcessed(String project, String revision, RefacToolType tool);
	
}
