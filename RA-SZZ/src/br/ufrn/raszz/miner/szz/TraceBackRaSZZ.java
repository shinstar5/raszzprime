package br.ufrn.raszz.miner.szz;

import static br.ufrn.raszz.util.FileOperationsUtil.getAdditionsInHunk;
import static br.ufrn.raszz.util.FileOperationsUtil.getDiffHunks;
import static br.ufrn.raszz.util.FileOperationsUtil.getHeaders;
import static br.ufrn.raszz.util.FileOperationsUtil.isCommentOrBlankLine;
import static br.ufrn.raszz.util.FileOperationsUtil.isImport;
import static br.ufrn.raszz.util.FileOperationsUtil.joinUnfinishedLines;
import static br.ufrn.raszz.util.FileOperationsUtil.prepareLineContent;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import br.ufrn.raszz.model.RefElement;
import br.ufrn.raszz.model.RefacToolType;
import br.ufrn.raszz.model.RepositoryType;
import br.ufrn.raszz.model.SZZImplementationType;
import br.ufrn.raszz.model.szz.AnnotationGraphModel;
import br.ufrn.raszz.model.szz.BugIntroducingCode;
import br.ufrn.raszz.model.szz.DiffHunk;
import br.ufrn.raszz.model.szz.Line;
import br.ufrn.raszz.model.szz.LineType;
import br.ufrn.raszz.model.szz.SzzFileRevision;
import br.ufrn.raszz.persistence.SzzDAO;
import br.ufrn.raszz.refactoring.RefacOperations;
import br.ufrn.razszz.connectoradapter.SzzRepository;

public class TraceBackRaSZZ extends AnnotationGraphService {
	
	private List<RefElement> refacSet;
	
	private Map<RefacToolType, Map<String, String>> refacRevProcSet;
	private RefacToolType refacTool;
			
	public TraceBackRaSZZ(SzzRepository repository, SzzDAO szzDao, String project, List<String> linkedRevs,
			String repoUrl, String debugPath, String debugContent, SZZImplementationType szzType, 
			RefacToolType refacTool, boolean isTest, List<String> processedRevisions, int threadId) {
		super(repository, szzDao, project, linkedRevs, repoUrl, debugPath, debugContent, szzType, isTest, processedRevisions, threadId);
		this.refacTool = refacTool;
		this.refacRevProcSet = new HashMap<>();
		this.refacSet = new ArrayList<RefElement>();
		recoverRefac(refacTool);
	}
		
	private void recoverRefac(RefacToolType refacTool) {
		if (refacTool == RefacToolType.BOTH) {
			recoverRefac(RefacToolType.REFDIFF);
			recoverRefac(RefacToolType.RMINER);
		} else {
			Map<String, String> refacToolRevProcSet = (linkedRevs.size() != 0)? 
					szzDAO.getAllRefacRevisionsProcessed(project, refacTool): new HashMap<String,String>();
			//if (refacToolRevProcSet != null && refacToolRevProcSet.size() != 0) 
				refacRevProcSet.put(refacTool, refacToolRevProcSet);
			if (linkedRevs.size() != 0)
				refacSet.addAll(szzDAO.getRefacBic(project, refacTool));
			//= (linkedRevs.size() != 0)? szzDAO.getRefacBic(project, refacTool): new ArrayList<RefElement>();
		}
	}
	
	@Override
	protected void traceBack(AnnotationGraphModel model, 
			LinkedList<SzzFileRevision> fileRevisions) throws Exception {
		final SzzFileRevision fixRev = fileRevisions.getLast(); 
		final SzzFileRevision beforeRev = fileRevisions.get(fileRevisions.indexOf(fixRev)-1);
		final ByteArrayOutputStream diff = repository.diffOperation(repoUrl, beforeRev, fixRev);

		//#checkerror
		/*if (fixRev.getRevision() == "429856") return;
		if (fixRev.getRevision() == "429863") return;
		if (fixRev.getRevision() == "429919") return;*/
				
		List<DiffHunk> hunks = null;
		List<String> headers = null;
		try{
			headers = getHeaders(diff);
			hunks = getDiffHunks(diff, 
					headers, 
					beforeRev.getPath(), 
					fixRev.getPath(), 
					beforeRev.getRevision(), 
					fixRev.getRevision());	
			joinUnfinishedLines(hunks);

			for(DiffHunk hunk : hunks){
				for(Line linetotrace : hunk.getContent()){
					//System.err.println(linetotrace.getRevision());
										
					//if (fixRev.getRevision() == 429919) continue;
					//if (linetotrace.getRevision() == 429891) continue;										
					//if (linetotrace.getRevision() == 429856) continue; 
					// Contorno de loop infinito na revision 429891 no projeto Derby
					
					if(linetotrace.getType() == LineType.DELETION){
						//#checkerror	
						if (checkerror(linetotrace)) continue;
						//log.info("line to trace: " +
						//linetotrace.getContent() +
						//"#" +
						//linetotrace.getPreviousNumber()
						//+ "- r:" +
						//linetotrace.getRevision() +
						//"- p:" +
						//linetotrace.getPreviousRevision());
						
						if(!isCommentOrBlankLine(linetotrace.getContent()) && !isImport(linetotrace.getContent())){   
					
							Line startlinetotrace = linetotrace;
							
							/*String fname = FileOperationsUtil.getFileName(linetotrace.getPreviousPath());
							//DIFFJ 
							Report diffJReport = DiffJOperationsUtil.diffJOperation(repository, repoUrl, beforeRev, fixRev, fname);	
							FileDiffs fds = diffJReport.getDifferences();
							for (FileDiff fileDiff
							: fds) {
							System.out.println(fileDiff.getType()
							+ ": " +
							fileDiff.getFirstLocation()
							+ " - " +
							fileDiff.getSecondLocation());
							}
							//diffJReport.printAll();	
							System.out.println("rev: " + beforeRev);
							System.out.println("linerev: " + linetotrace.getRevision());
							diffJReport.printAll();	
							System.out.println("previuoslinerev: " + linetotrace.getPreviousRevision());
							FileDiff fileDiffJ = DiffJOperationsUtil.hasDiffJType(diffJReport, linetotrace);
							if (fileDiffJ != null) {
								//b.setDiffjmessage(fileDiffJ.getMessage());
								//b.setDiffjlocation(fileDiffJ.getFirstLocation().toString());
								log.info(" - diffJ: " + fileDiffJ.toDiffSummaryString() + " - " + fileDiffJ.getMessage());
							}	
							//DIFFJ	*/													
							
							//REFACTORING IN BUG-FIX CHANGES				
							//check if the refactoring is stored 
							String commitId = linetotrace.getPreviousRevision();
							RefacOperations.checkRefactoringStored(repository, commitId, project, refacTool, refacRevProcSet, refacSet);
							//check if there are refactorings in the fixing
							boolean isrefac =
								szzDAO.hasRefacFix(beforeRev.getPath(),
										fixRev.getRevision(),
										linetotrace.getPreviousNumber(),
										linetotrace.getAdjustmentIndex(),
										linetotrace.getContent());
							if (isrefac) {
								log.debug(" Detected REFACTORING in line " + 
										linetotrace.getPreviousNumber() + 
										" of the file " + beforeRev.getPath() + " (" + fixRev.getRevision() + ")");
								break;
							} else {								
								createLinesInPreviousRevisions(model,linetotrace, beforeRev, fileRevisions, fixRev, false,
										0, 0, false, null, 1, startlinetotrace, linetotrace.getPreviousPath());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			diff.close();
		}
	}

	private SzzFileRevision getPrevRev(SzzFileRevision fileRevision, List<SzzFileRevision> revisions){
		SzzFileRevision prev = null;
		int index = revisions.indexOf(fileRevision);
		//in case the file is not  the first of the collection
		if(index > 0)
			prev = revisions.get(index-1);
		return prev;
	}
	
	private void createLinesInPreviousRevisions(AnnotationGraphModel model, Line linetotrace, SzzFileRevision rev,
					LinkedList<SzzFileRevision> fileRevisions, SzzFileRevision fixRev, boolean isrefac, int indexPosRefac,
					int indexChangePath, boolean isReTrace, SzzFileRevision reprevrev, int indexFurtherBack, Line startline,
					String currentPath) 
					throws Exception {
		Line prevline = null;		
		//log.info(String.format("createLinesInPreviousRev(184) - analyzed revision: %s",fixRev.getRevision()));
		String content = prepareLineContent(linetotrace);
		SzzFileRevision prevrev = (!isReTrace)? getPrevRev(rev,fileRevisions) : reprevrev;
		//if the buggycode is in rev from the start
		//we have to persist it when prevrev == null
		if(prevrev != null) {
			LinkedList<Line> previousLines = model.get(prevrev); 
			
			//#DEBUG
			if (debugContent != null && linetotrace.getContent().equals(debugContent)) 
				log.debug("DEBUG POINT IN: " + prevrev.getRevision());
						
			//REFACTORING IN FIX-INDUNCING CHANGES 				
			//verifica se a refatorao nao ta salva
			if (indexPosRefac >= 0) {
				String commitId = linetotrace.getPreviousRevision();
				RefacOperations.checkRefactoringStored(repository, commitId, project, refacTool, refacRevProcSet, refacSet);
			}			
			//verifica se contm refatorao no arquivo/revisao analisado
			List<RefElement> currentRefacSet = RefacOperations.filterRefacSet(refacSet, linetotrace.getPreviousPath(), linetotrace.getPreviousRevision(), linetotrace.getPreviousNumber(), linetotrace.getAdjustmentIndex(), content); 
			if (currentRefacSet != null && currentRefacSet.size() != 0) {
				String prevrefpath = RefacOperations.prevRefacPath(refacSet, linetotrace.getPreviousPath(), linetotrace.getPreviousRevision(), linetotrace.getPreviousNumber(), linetotrace.getAdjustmentIndex(), content);
				if (prevrefpath != null && !prevrev.getPath().equals(prevrefpath)) {
					retrace(model, linetotrace, fileRevisions, fixRev, isrefac, indexPosRefac, indexChangePath, rev, prevrefpath, indexFurtherBack, startline);
					return;
				}
				String prevrefcontent = RefacOperations.prevRefacContent(currentRefacSet, content);
				content = prevrefcontent;
				isrefac = true;
			} else isrefac = false;			
			indexPosRefac += (isrefac || indexPosRefac >0)?1:0;
			//log.info(linetotrace.getPreviousRevision()+"#"+indexPosRefac);
			//END REFACTORING IN FIX-INDUNCING CHANGES
			
			boolean matchFound = false;
			for(Line line : previousLines) {				
				String prevcontent = prepareLineContent(line);

				/*//#DEBUG
				if (prevcontent.equals(debugContent)) {
				//if (linetotrace.getPreviousRevision().equals("bbf3ed85e0f668331edb269329bf577fe27932a8")) {
				//if (prevrev.getRevision().equals("57533")) { 
					log.info("line to trace: " + content + "#" + linetotrace.getPreviousNumber() + "- r:" + linetotrace.getRevision() + "- p:" + linetotrace.getPreviousRevision());
					log.info("prevline     : " + prevcontent + "#p" + line.getPreviousNumber() + "-#n"+ line.getNumber() + "- r:" + line.getRevision() + "- p:" + line.getPreviousRevision());
				}*/
							
				if(content.equals(prevcontent)){
					if (isrefac) {	
						long[] interval = RefacOperations.prevRefacLines(currentRefacSet, szzDAO);
						if(interval[0] <= line.getPreviousNumber() && line.getPreviousNumber() <= interval[1]){
							prevline = line; 
							if(prevline != null){
								matchFound = true;
								//found match by refactoring
								createLinesInPreviousRevisions(model, prevline, prevrev, fileRevisions, 
										fixRev, isrefac, indexPosRefac, indexChangePath, false, null, ++indexFurtherBack, startline, currentPath);
								break;
							}
						}
					} else {			
						if(line.getNumber() != -1){
							//we have to find where the exact code was introduced this is why we don't care about evolutions array						
							//because it means that the code have changed from the previous revision
							if(line.getNumber() == linetotrace.getPreviousNumber()){
								prevline = line; 
								if(prevline != null){
									matchFound = true;
									//log.info(" found a match to [" + line.getNumber() + ", rev: " + prevrev.getRevision() + "] prev_line_content = " + prevcontent);
									//recursive call to traceback								
									createLinesInPreviousRevisions(model, prevline, prevrev, fileRevisions, 
											fixRev, isrefac, indexPosRefac, indexChangePath, false, null, ++indexFurtherBack, startline, currentPath);
									break;
								}
							} 
						} else if( linetotrace.getPreviousNumber() == (line.getPreviousNumber()
									+line.getContext_difference() + line.getPosition())) {
							prevline = line;
							if(prevline != null){
								matchFound = true;
								//c.readLine("found match by content and context adjustment!");
								//log.info("found match by content and context adjustment in rev: " + prevrev.getRevision());
								//recursive call to traceback
								createLinesInPreviousRevisions(model, prevline, prevrev, fileRevisions, 
										fixRev, isrefac, indexPosRefac, indexChangePath, false, null, ++indexFurtherBack, startline, currentPath);
								break;
							}
						} else { //last match attempt
							//#debug
							//c.readLine("last match attempt... trying to do evolution tracing");
							List<Integer> additions = getAdditionsInHunk(line);
	
							if(!additions.isEmpty()){
								boolean internalMatch = false;
								for(Integer addition : additions){
									int position = addition - line.getDeletions();
									if(linetotrace.getPreviousNumber() == (line.getPreviousNumber() + 
												line.getContext_difference() + position)){
										prevline = line;
										if(prevline != null){
											matchFound = true;
											//log.info("found match by evolution trace!");
											createLinesInPreviousRevisions(model, prevline, prevrev, fileRevisions, 
													fixRev, isrefac, indexPosRefac, indexChangePath, false, null, ++indexFurtherBack, startline, currentPath);
											internalMatch = true; 
											break;
										}
									}
								}
								if (internalMatch) break;
							} else {
								//#debug
								//c.readLine("trying local evolution trace!");							
								int position = 0;
								if(line.getContext_difference() > 0)
									position = line.getDeletions() - line.getAdditions();
								else
									position = line.getAdditions() - line.getDeletions();
	
								if(linetotrace.getPreviousNumber() == (line.getPreviousNumber() + line.getContext_difference() + position)){
									prevline = line;
									if(prevline != null){
										matchFound = true;
										//log.info("found match by local evolution trace!");
										createLinesInPreviousRevisions(model, prevline, prevrev, fileRevisions, 
												fixRev, isrefac, indexPosRefac, indexChangePath, false, null, ++indexFurtherBack, startline, currentPath);
										break;
									}
							    }
							}	
						}
					}
				} else {					
					//se nao for igual mas for refac?
					//mas precisa ser um refac do conserto
					//e como descobrir a versao anterior do codigo antes do refac?
					
				}
			} 
			if(!matchFound){
				createBicode(rev, fixRev, linetotrace, indexPosRefac, indexChangePath, isrefac, indexFurtherBack, startline);				
			}
		} else {
			//IF mudou de local o arquivo?	
			//verifica se a refatorao nao ta salva
			if (indexPosRefac >= 0) {
				String commitId = linetotrace.getPreviousRevision();
				RefacOperations.checkRefactoringStored(repository, commitId, project, refacTool, refacRevProcSet, refacSet);
			}	
			//#RE-TRACE	
			String prevrefpath = null;
			String currentRev = linetotrace.getPreviousRevision();
			prevrefpath = RefacOperations.prevRefacPath(refacSet, currentPath, currentRev, linetotrace.getPreviousNumber(), linetotrace.getAdjustmentIndex(), content);			
			if (prevrefpath != null) {
				retrace(model, linetotrace, fileRevisions, fixRev, isrefac, indexPosRefac, indexChangePath, rev, prevrefpath, indexFurtherBack, startline);
			} else {
				createBicode(rev, fixRev, linetotrace, indexPosRefac, indexChangePath, isrefac, indexFurtherBack, startline);
			}
		}
	}
	
	private void retrace(AnnotationGraphModel model, Line linetotrace, LinkedList<SzzFileRevision> fileRevisions, SzzFileRevision fixRev, boolean isrefac, int indexPosRefac,
			int indexChangePath, SzzFileRevision rev, String prevrefpath, int indexFurtherBack, Line startline) throws Exception{
		
		if (repository.getConnectorType() == RepositoryType.SVN)
			fileRevisions = repository.extractSZZFilesFromPath(repoUrl, prevrefpath, (Long.parseLong(linetotrace.getPreviousRevision())-1)+"", true);
			//fileRevisions = repository.extractSZZFilesFromPath(repoUrl, prevrefpath, (Long.parseLong(currentRev)-1)+"", true);
		else if (repository.getConnectorType() == RepositoryType.GIT) {
			String previousRev = repository.getPreviousRevision(repoUrl, prevrefpath, linetotrace.getPreviousRevision());
			//String previousRev = repository.getPreviousRevision(repoUrl, prevrefpath, currentRev);
			fileRevisions = repository.extractSZZFilesFromPath(repoUrl, prevrefpath, previousRev, true);			
		}
		if (fileRevisions != null) {
			AnnotationGraphBuilder agb = new AnnotationGraphBuilder();
			model = agb.buildLinesModel(repository, fileRevisions, repoUrl, project);
			SzzFileRevision reprevrev = fileRevisions.getLast();
			createLinesInPreviousRevisions(model, linetotrace, rev, fileRevisions, 
					fixRev, isrefac, indexPosRefac, ++indexChangePath, true, reprevrev, ++indexFurtherBack, startline, prevrefpath);
		}
	}
	
	private BugIntroducingCode createBicode(SzzFileRevision rev, SzzFileRevision fixRev, Line line, 
			int indexPosRefac, int indexChangePath, boolean isrefac, int indexFurtherBack, Line startline){
		BugIntroducingCode b = new BugIntroducingCode();
		
			/*//DIFFJ 
			Report diffJReport = DiffJOperationsUtil.diffJOperation(repoUrl, rev, fixRev, fname);	
			//diffJReport.printAll();	
			//System.out.println("rev: " + rev);
			//System.out.println("linerev: " + line.getRevision());
			//System.out.println("previuoslinerev: " + line.getPreviousRevision());
			FileDiff fileDiffJ = DiffJOperationsUtil.hasDiffJType(diffJReport, line);
			if (fileDiffJ != null) {
				b.setDiffjmessage(fileDiffJ.getMessage());
				b.setDiffjlocation(fileDiffJ.getFirstLocation().toString());
				log.info(" - diffJ: " + fileDiffJ.toDiffSummaryString() + " - " + fileDiffJ.getMessage());
			}	
			//DIFFJ	*/	
		
			//if (line.getContent().equals("+          return (String) ref;"))
			//		log.info(line.getNumber());
			
		
			b.setFixRevision(fixRev.getRevision());
			b.setContent(line.getContent());
			//if (line.getContentAdjusted()) {
			//	log.info(line.getPreviousNumber() + " - " + (line.getPreviousNumber() + line.getAdjustmentIndex()));
			//}
			b.setLinenumber(line.getPreviousNumber());
			b.setPath(rev.getPath());
			b.setRevision(rev.getRevision());
			
			b.setProject(this.project);
			b.setAdjustmentIndex(line.getAdjustmentIndex());
			b.setIndexPosRefac(indexPosRefac);
			b.setIndexChangePath(indexChangePath);
			//String toparse = rev.getRevisionProperties().getStringValue(SVNRevisionProperty.DATE);
			//Date creation = FileOperationsUtil.getRevisionDate(toparse);
			//b.setSzzDate(creation);
			b.setSzzDate(rev.getCreateDate());
			b.setIsrefac(isrefac);
			b.setIndexFurtherBack(indexFurtherBack);
			
			b.setStartRevision(startline.getPreviousRevision());
			b.setStartContent(startline.getContent());
			b.setStartPath(startline.getPreviousPath());
			b.setStartlinenumber(startline.getPreviousNumber());
		
		bicodes.add(b);
		return b;
	}
	
	private boolean checkerror(Line linetotrace){
		if (linetotrace.getPreviousRevision() == "528546") {
				System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
				System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
				return true;
		} else if ((linetotrace.getPreviousNumber() == 1373 || linetotrace.getPreviousNumber() == 1375 || linetotrace.getPreviousNumber() == 1376) && linetotrace.getPreviousRevision() == "528546" &&
			linetotrace.getPreviousPath().equals("/db/derby/code/trunk/java/shared/org/apache/derby/shared/common/reference/SQLState.java")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if ((linetotrace.getPreviousNumber() == 1368) && linetotrace.getPreviousRevision() == "528546" &&
				linetotrace.getPreviousPath().equals("/db/derby/code/trunk/java/shared/org/apache/derby/shared/common/reference/SQLState.java")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() != 1474 && linetotrace.getPreviousRevision() == "1352631" &&
				linetotrace.getPreviousPath().equals("/db/derby/code/trunk/java/shared/org/apache/derby/shared/common/reference/SQLState.java")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 1 &&
				linetotrace.getContent().equals("<%@jetpackage=\"org.apache.tuscany.sdo.generate.templates.model\"skeleton=\"generator.skeleton\"imports=\"org.eclipse.emf.codegen.util.*java.util.*org.eclipse.emf.codegen.ecore.genmodel.*\"class=\"SDOClass\"version=\"$Id:Class.javajet,v1.412006/02/1519:58:39emerksExp$\"%><%@jetpackage=\"org.apache.tuscany.sdo.generate.templates.model\"skeleton=\"generator.skeleton\"imports=\"org.apache.tuscany.sdo.generate.util.*java.util.*org.eclipse.emf.codegen.ecore.genmodel.*\"class=\"SDOClass\"version=\"$Id:Class.javajet,v1.412006/02/1519:58:39emerksExp$\"%><%/***")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 941 &&
		linetotrace.getContent().equals("-			<%=genModel.getImportedName(\"org.eclipse.emf.ecore.impl.ENotificationImpl\")%> notification = new <%=genModel.getImportedName(\"org.eclipse.emf.ecore.impl.ENotificationImpl\")%>(this, <%=genModel.getImportedName(\"org.eclipse.emf.common.notify.Notification\")%>.UNSET, <%=genFeature.getUpperName()%>, <%if (genModel.isVirtualDelegation()) {%>isSetChange ? old<%=genFeature.getCapName()%> : null<%} else {%>old<%=genFeature.getCapName()%><%}%>, null, <%if (genModel.isVirtualDelegation()) {%>isSetChange<%} else {%>old<%=genFeature.getCapName()%>_set_<%}%>);")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 942 &&
		linetotrace.getContent().equals("-			if (changeContext == null) changeContext = notification; else changeContext.add(notification);")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 1003 &&
		linetotrace.getContent().equals("<%if(!genFeature.isBidirectional()){%>changeContext=((<%=genModel.getImportedName(\"org.eclipse.emf.ecore.InternalEObject\")%>)<%=genFeature.getSafeName()%>).inverseRemove(this,EOPPOSITE_FEATURE_BASE-<%=genFeature.getUpperName()%>,null,changeContext);")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 1005 &&
		linetotrace.getContent().equals("<%}else{GenFeaturereverseFeature=genFeature.getReverse();GenClasstargetClass=reverseFeature.getGenClass();%>changeContext=((<%=genModel.getImportedName(\"org.eclipse.emf.ecore.InternalEObject\")%>)<%=genFeature.getSafeName()%>).inverseRemove(this,<%=targetClass.getQualifiedFeatureID(reverseFeature)%>,<%=targetClass.getImportedInterfaceName()%>.class,changeContext);")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 1443 &&
		linetotrace.getContent().equals("-				return eInternalContainer().inverseRemove(this, <%=targetClass.getQualifiedFeatureID(reverseFeature)%>, <%=targetClass.getImportedInterfaceName()%>.class, changeContext);")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 192 &&
		linetotrace.getContent().equals("-           featureValue = base.getClassName() + \".\" + genFeature.getUpperName();")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 194 &&
		linetotrace.getContent().equals("-           String baseCountID = base.getClassName() + \".\" + \"SDO_PROPERTY_COUNT\";")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 213 &&
		linetotrace.getContent().equals("-           featureValue = base.getClassName() + \".\" + genFeature.getUpperName();")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 215 &&
		linetotrace.getContent().equals("-           String baseCountID = base.getClassName() + \".\" + \"EXTENDED_PROPERTY_COUNT\";")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 290 &&
		linetotrace.getContent().equals("-      String baseCountID = base.getClassName() + \".\" + \"INTERNAL_PROPERTY_COUNT\";")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 346 &&
		linetotrace.getContent().equals("protectedstaticfinal<%=genFeature.getImportedType()%><%=genFeature.getUpperName()%>_DEFAULT_=<%=genFeature.getStaticDefaultValue()%>;<%=genModel.getNonNLS(genFeature.getStaticDefaultValue())%>protectedstaticfinal<%=genFeature.getImportedType()%><%=genFeature.getUpperName()%>_DEFAULT_=<%=SDOGenUtil.getStaticDefaultValue(genFeature)%>;<%=genModel.getNonNLS(genFeature.getStaticDefaultValue())%><%}%><%if(genClass.isField(genFeature)){%>")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 8 &&
		linetotrace.getContent().equals("youmaynotusethisfileexceptincompliancewiththeLicense.YoumayobtainacopyoftheLicenseatLicensedtotheApacheSoftwareFoundation(ASF)underoneormorecontributorlicenseagreements.SeetheNOTICEfiledistributedwiththisworkforadditionalinformationregardingcopyrightownership.TheASFlicensesthisfiletoyouundertheApacheLicense,Version2.0(the\"License\");youmaynotusethisfileexceptincompliancewiththeLicense.YoumayobtainacopyoftheLicenseathttp://www.apache.org/licenses/LICENSE-2.0")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 276 &&
		linetotrace.getContent().equals("-  public static final BinaryFunction logBeta = new BinaryFunction() {")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 277 &&
		linetotrace.getContent().equals("-    public final double apply(double a, double b) { return Sfun.logBeta(a,b); }")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 278 &&
		linetotrace.getContent().equals("-  };")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 1055 &&
		linetotrace.getContent().equals("-            e.printStackTrace(agent.getServer().logWriter);")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 182 &&
		linetotrace.getContent().equals("-				break;")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		} else if (linetotrace.getPreviousNumber() == 1156 
				&& linetotrace.getContent().equals("-	String RTS_USER_SUPPLIED_OPTIMIZER_OVERRIDES_FOR_JOIN			   = \"43Y57.U\"; ")) {
			System.err.println("CAIU NO IF DO CONSERTO DO ESTOURO DE PILHA");
			System.err.println("line: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber());
			return true;
		}
		return false;
	}
}
