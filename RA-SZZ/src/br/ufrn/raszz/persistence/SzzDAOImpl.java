package br.ufrn.raszz.persistence;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.SQLQuery;

import br.ufrn.raszz.model.RefElement;
import br.ufrn.raszz.model.Refac;
import br.ufrn.raszz.model.RefacToolType;
import br.ufrn.raszz.model.SZZImplementationType;
import br.ufrn.raszz.model.szz.BugIntroducingCode;

public class SzzDAOImpl extends SzzDAO {
	
	@Override
	public synchronized List<String> getLinkedRevisions(String project) {
		String sql = "select distinct revisionnumber, cast(commitdate as timestamp) from linkedissuessvn lsvn " +
			"where projectname like :project " +
			"and issuetype = 'Bug' " +
		//	"and issuecode like 'TUSCANY-1867'" + //onlytest
		//	"and revisionnumber = '790999' " + //'698203' " + // '638810' " +  //'1045314' " + //'698203' " + '404728' " + // '521426'" + // '1466557'" + //onlytest 
			"order by cast(commitdate as timestamp)";

		List<String> revisionsConverted = new ArrayList<String>();
		List<Object[]> revisions = new ArrayList<Object[]>();
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("project", project);
		revisions = query.list();

		for (Object[] revision : revisions) {
			String revisionconverted = revision[0].toString();
			revisionsConverted.add(revisionconverted);
		}
		return revisionsConverted;
	}
	
	@Override
	public synchronized List<String> getLinkedRevisionsWProcessedBIC(String project) {
		String sql = "select distinct revisionnumber, cast(commitdate as timestamp) from linkedissuessvn "
				+ "where projectname like :project "
				+ "and revisionnumber in (select distinct cast(fixrevision as text) from bicszzse) "
				//+ "and revisionnumber = '453123' "
				//+ "and revisionnumber not in (select distinct cast(fixrevision as text) from bicszzse2)"
				+ "order by cast(commitdate as timestamp)";

		List<String> revisionsConverted = new ArrayList<String>();
		List<Object[]> revisions = new ArrayList<Object[]>();
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("project", project);
		revisions = query.list();

		for (Object[] revision : revisions) {
			String revisionconverted = revision[0].toString();
			revisionsConverted.add(revisionconverted);
		}
		return revisionsConverted;
	}
	
	@Override
	public synchronized List<String> getGitLinkedRevision(String project) {
		String sql = "select distinct fixed_revision from linkedissuegit lgit " +
			"where project like :project";

		List<String> revisions = new ArrayList<String>();
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("project", project);
		revisions = query.list();
		return revisions;
	}
	
	@Override
	public synchronized List<String> getLinkedRevisionWAffectedVersions(String project) {
		String sql = "select distinct revisionnumber, cast(commitdate as timestamp) from linkedissuessvn lsvn " +
			"inner join issuecontents ic on lsvn.issuecode = ic.bug_id " +
			"inner join issuecontents_affectedversions ica on ic.id = ica.issuecontents_id " +
			"inner join release r on ica.affectedversions = r.version and lsvn.projectname like r.project " +
			"where projectname like :project " +
			"and lsvn.issuetype = 'Bug' " +
			"order by cast(commitdate as timestamp)";

		List<String> revisionsConverted = new ArrayList<String>();
		List<Object[]> revisions = new ArrayList<Object[]>();
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("project", project);
		revisions = query.list();

		for (Object[] revision : revisions) {
			String revisionconverted = revision[0].toString();
			revisionsConverted.add(revisionconverted);
		}
		return revisionsConverted;
	}

	public synchronized List<String> getAllRevisionProcessed(String project){
		String sql = "select lastrevisionprocessed from szz_project_lastrevisionprocessed " +
			"where project = :project";
		List<String> revisionsConverted = new ArrayList<String>();
		List<Object[]> revisions = new ArrayList<Object[]>();
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("project", project);
		revisions = query.list();
		for (Object revision : revisions) {
			String revisionconverted = revision.toString();
			revisionsConverted.add(revisionconverted);
		}
		return revisionsConverted;
		
	}
	
	@Override
	public synchronized Map<String,String> getAllRefacRevisionsProcessed(String project, RefacToolType tool) {
		String sql = "select project, revision from szz_refac_revisionprocessed "
				+ "where project = :project and tool = :tool";
		Map<String,String> revisionsConverted = new HashMap<String,String>();
		List<Object[]> revisions = new ArrayList<Object[]>();
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("project", project);
		query.setParameter("tool", tool.toString());
		revisions = query.list();
		for (Object[] revision : revisions) {
			if (revision[1] != null) {
				String projectconverted = revision[0].toString();
				String revisionconverted = revision[1].toString();
				revisionsConverted.put(revisionconverted, projectconverted);
			}
		}
		return revisionsConverted;
	}
	
	@Override
	public synchronized void insertProjectRevisionsProcessed(String project, String revision){
		String sql = "insert into szz_project_lastrevisionprocessed values (:param1,:param2)";
		executeSQLWithParams(sql,project, revision);
	}

	@Override
	public synchronized void insertBugIntroducingCode(BugIntroducingCode bicode, SZZImplementationType szzType){
		switch (szzType) {
		case RASZZ:
			insertRABugIntroducingCode(bicode);
			break;
		case MASZZ:
			insertMABugIntroducingCode(bicode);
			break;									
		}		
	}
		
	private synchronized void insertRABugIntroducingCode(BugIntroducingCode bicode){
		String sql = "INSERT INTO bicraszzgit (linenumber, path, content, revision, "
				+ "fixrevision, project, szz_date, copypath, copyrevision, mergerev, branchrev, "
				+ "changeproperty, missed, furtherback, diffjmessage, diffjlocation, adjustmentindex, "
				+ "indexposrefac, indexchangepath, isrefac, indexfurtherback, "
				+ "startrevision, startpath, startlinenumber, startcontent) "
				+ "values (:param1,:param2, :param3, :param4, :param5, :param6, :param7, :param8,"
				+ " :param9, :param10, :param11, :param12,:param13, :param14, :param15, :param16, "
				+ ":param17, :param18, :param19, :param20, :param21, "
				+ ":param22, :param23, :param24, :param25)";
		executeSQLWithParams(sql,bicode.getLinenumber(), bicode.getPath(), bicode.getContent(),
				bicode.getRevision(), bicode.getFixRevision(), bicode.getProject(), 
				bicode.getSzzDate(),bicode.getCopypath(),bicode.getCopyrevision(),
				bicode.getMergerev(),bicode.getBranchrev(),bicode.getChangeproperty(),
				bicode.getMissed(),bicode.getFurtherback(), bicode.getDiffjmessage(), 
				bicode.getDiffjlocation(), bicode.getAdjustmentIndex(), 
				bicode.getIndexPosRefac(), bicode.getIndexChangePath(),
				bicode.isIsrefac(), bicode.getIndexFurtherBack(),
				bicode.getStartRevision(), bicode.getStartPath(),
				bicode.getStartlinenumber(), bicode.getStartContent());	       
	}
	
	private synchronized void insertMABugIntroducingCode(BugIntroducingCode bicode){
		String sql = "INSERT INTO bicmaszztest(linenumber, path, content, revision, "
				+ "fixrevision, project, szz_date, copypath, copyrevision, mergerev, branchrev, "
				+ "changeproperty, missed, furtherback, diffjmessage, diffjlocation, indexfurtherback, "
				+ "startrevision, startpath, startlinenumber, startcontent) "
				+ "values (:param1,:param2, :param3, :param4, :param5, :param6, :param7, :param8,"
				+ " :param9, :param10, :param11, :param12,:param13, :param14, :param15, :param16,"
				+ " :param17, :param18, :param19, :param20, :param21)";
		executeSQLWithParams(sql,bicode.getLinenumber(), bicode.getPath(), bicode.getContent(),
				bicode.getRevision(), bicode.getFixRevision(), bicode.getProject(), 
				bicode.getSzzDate(),bicode.getCopypath(),bicode.getCopyrevision(),
				bicode.getMergerev(),bicode.getBranchrev(),bicode.getChangeproperty(),
				bicode.getMissed(),bicode.getFurtherback(), bicode.getDiffjmessage(), 
				bicode.getDiffjlocation(), bicode.getIndexFurtherBack(),
				bicode.getStartRevision(), bicode.getStartPath(),
				bicode.getStartlinenumber(), bicode.getStartContent());	       
	}	
	
	@Override
	public synchronized boolean hasRefacFix(String path, String fixrevision, int linenumber, int adjustmentindex, String content) {
		String sql = "SELECT count(distinct(refactoringtype, entitybefore, entityafter, elementtype, afterstartline, afterendline, afterstartscope,"
				+ " afternestinglevel, beforestartscope, beforestartline, beforeendline, tool)) " +
						"FROM refdiffresult ref " +
						"WHERE (:fixrevision = ref.revision " + 
							"AND :path = ref.beforepathfile " +
						      ") " +
							"AND " +
						      "( " +
							"(refactoringtype IN ('EXTRACT_INTERFACE') AND :content like concat('%class%',REF.beforesimplename,'%')) OR " +
							"(refactoringtype IN ('EXTRACT_SUPERCLASS') AND :content like concat('%class%',REF.beforesimplename,'%')) OR " +
							"(refactoringtype IN ('MOVE_ATTRIBUTE') AND (:linenumber + :adjustmentindex) >= REF.beforestartline AND :linenumber <= REF.beforeendline) OR " +	
							"(refactoringtype IN ('MOVE_OPERATION') AND (:linenumber + :adjustmentindex) >= REF.beforestartline AND :linenumber <= REF.beforeendline) OR " +	
							"(refactoringtype IN ('MOVE_CLASS') AND (:linenumber + :adjustmentindex) >= REF.beforestartline AND :linenumber <= REF.beforeendline) OR " +
							"(refactoringtype IN ('PULL_UP_ATTRIBUTE') AND (:linenumber + :adjustmentindex) >= REF.beforestartline AND :linenumber <= REF.beforeendline) OR " +
							//"(refactoringtype IN ('PULL_UP_OPERATION') AND (:linenumber + :adjustmentindex) >= REF.beforestartline AND :linenumber <= REF.beforeendline) OR " +
							"(refactoringtype IN ('PUSH_DOWN_ATTRIBUTE') AND (:linenumber + :adjustmentindex) >= REF.beforestartline AND :linenumber <= REF.beforeendline) OR " +
							//"(refactoringtype IN ('PUSH_DOWN_OPERATION') AND (:linenumber + :adjustmentindex) >= REF.beforestartline AND :linenumber <= REF.beforeendline) OR " +
							"(refactoringtype IN ('RENAME_METHOD') AND :linenumber >= REF.beforestartline AND :linenumber < REF.beforestartscope) OR " +
							"(refactoringtype IN ('RENAME_CLASS') AND (:linenumber >= beforestartline " + 
								"AND (:linenumber <= REF.beforestartscope OR (:linenumber = REF.beforestartscope+1 AND (:content like '%class%' or :content like '%interface%' or :content like '%enum%'))) " +
								")) OR " +
							"(refactoringtype IN ('MOVE_RENAME_CLASS') AND (REF.beforenestinglevel = 0 AND :linenumber >= 1 " + 
								"AND (:linenumber <= REF.beforestartscope OR (:linenumber = REF.beforestartscope+1 AND (:content like '%class%' or :content like '%interface%' or :content like '%enum%'))) " +
								")) OR " +
							"(refactoringtype IN ('MOVE_RENAME_CLASS') AND (REF.beforenestinglevel >= 0 AND :linenumber >=beforestartline and (:linenumber <= REF.beforestartscope OR (:linenumber = REF.beforestartscope+1 AND (:content like '%class%' or :content like '%interface%' or :content like '%enum%'))) " +
								")) OR " +
							"(refactoringtype IN ('MOVE_RENAME_CLASS', 'RENAME_CLASS') AND (:content like concat('%class%',REF.beforesimplename,'%') OR :content like concat('%interface%',REF.beforesimplename,'%') OR :content like concat('%enum%',REF.beforesimplename,'%')) " + 
								") " +
							   " ) UNION ALL "+
							   "SELECT count(ref.*) FROM callerrefdiff ref " + 
							   "WHERE (:fixrevision = ref.revision " +
							    "AND REF.refactoringtype NOT IN ('EXTRACT_OPERATION', 'PULL_UP_OPERATION', 'PUSH_DOWN_OPERATION',"
							    + " 'PULL_UP_ATTRIBUTE', 'PUSH_DOWN_ATTRIBUTE', 'MOVE_OPERATION', 'MOVE_ATTRIBUTE', 'MOVE_CLASS',"
							    + " 'MOVE_RENAME_CLASS') " +
							   	"AND REF.type = 'before' " +
							   	//"AND revisiontype like 'fix%' " +
							   	"AND :path = REF.callerpath " +
							   	//"AND REF.tool = 'refdiff' " +		//por enquanto s refdiff
							   	"AND :linenumber = REF.callerline)";
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("path", path);
		query.setParameter("fixrevision", fixrevision);
		query.setParameter("linenumber", linenumber);
		query.setParameter("adjustmentindex", adjustmentindex);  
		query.setParameter("content", content);
		List<BigInteger> countref = query.list();
		if (countref.get(0).intValue() == 0 && countref.get(1).intValue() == 0 )
			return false;
		else
			return true;
	}
		
	@Override
	public synchronized List<RefElement> getRefacBicByRevision(String revision, String project) {
		String sql = "SELECT distinct revision, refactoringtype, entitybefore, entityafter, elementtype, afterstartline, afterendline, afterstartscope,"
				+ " aftersimplename, afterpathfile, afternestinglevel, beforepathfile, beforestartscope, beforestartline, beforeendline, tool " +
						"FROM refdiffresult ref " +
						"WHERE (:revision = ref.revision " + 
							"AND (ref.revisiontype like 'new%' OR ref.revisiontype like 'run')" +
						      ") ";
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("revision", revision);
		List<Object[]> results = query.list();
		return preencher(results, project);				
	}
	
	private List<RefElement> preencher(List<Object[]> results, String project){
		List<RefElement> refacs = new ArrayList<RefElement>();
		for (Object[] result : results) {		
			//RefElement refac = new RefElement(Long.parseLong(result[0].toString()), project);
			RefElement refac = new RefElement(result[0].toString(), project);
			refac.setRefactoringtype(result[1].toString());
			//if (refac.getEntitybefore() != null) 
				refac.setEntitybefore(result[2].toString());
			//else continue;//refac.setEntitybefore("");
			//if (refac.getEntityafter() != null)
				refac.setEntityafter(result[3].toString());
			//else continue;//refac.setEntityafter("");
			refac.setElementtype(result[4].toString());
			refac.setAfterstartline(Long.parseLong(result[5].toString()));
			refac.setAfterendline(Long.parseLong(result[6].toString()));
			refac.setAfterstartscope(Long.parseLong(result[7].toString()));
			//if (refac.getAftersimplename() != null)
				refac.setAftersimpleName(result[8].toString());
			//else continue;refac.setAftersimpleName("");
			refac.setAfterpathfile(result[9].toString());
			refac.setAfternestingLevel(Integer.parseInt(result[10].toString()));
			refac.setBeforepathfile(result[11].toString());
			refac.setBeforestarscope(Integer.parseInt(result[12].toString()));
			refac.setBeforestartline(Integer.parseInt(result[13].toString()));
			refac.setBeforeendline(Integer.parseInt(result[14].toString()));
			switch (result[15].toString()) {
			case "refdiff":
				refac.setTool(RefacToolType.REFDIFF);
				break;
			case "rminer":
				refac.setTool(RefacToolType.RMINER);
				break;
			}
			refac.setSummary(result[16].toString());
			refacs.add(refac);
		}
		return refacs;
	}
	
	@Override
	public synchronized List<RefElement> getRefacBic(String project, RefacToolType tool) {
		String sql = "SELECT distinct revision, refactoringtype, entitybefore, entityafter, elementtype, afterstartline, afterendline, afterstartscope,"
				+ " aftersimplename, afterpathfile, afternestinglevel, beforepathfile, beforestartscope, beforestartline, beforeendline, tool, summary " +
						"FROM refdiffresult ref " +
						"WHERE (:project = ref.project and :tool = ref.tool " + 
							"AND (ref.revisiontype like 'new%' OR ref.revisiontype like 'run')" +
						      ") " +						
					  "UNION ALL "+
						 "SELECT distinct cal.revision, cal.refactoringtype, ref.entitybefore, cal.entityafter, 'CALLER', cal.callerstartline, cal.callerendline, cal.callerline, "
						 + "cal.simplename, cal.callerpath, cal.nestinglevel, '', 0, 0, 0, cal.tool, cal.summary " + 
						   "FROM callerrefdiff cal inner join refdiffresult ref on (ref.summary = cal.summary) " + 
							   "WHERE (:project = cal.project and :tool = cal.tool " + 
							   	"AND ((cal.refactoringtype in('INLINE_OPERATION') and cal.type = 'before')" //quando for inline, trazer linhas removidas (after) ao inves de adicionadas
							   	+ "    OR (cal.refactoringtype not in ('INLINE_OPERATION') and cal.type = 'after'))" + 
							   	"AND (cal.revisiontype like 'new%' OR ref.revisiontype like 'run'))";
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("project", project);
		query.setParameter("tool", tool.toString());
		List<Object[]> results = query.list();
		return preencher(results, project);			
	}
	
	@Override
	public synchronized RefElement getRefacBicBySummary(String project, String summary) {
		String sql = "SELECT distinct revision, refactoringtype, entitybefore, entityafter, elementtype, afterstartline, afterendline, afterstartscope,"
				+ " aftersimplename, afterpathfile, afternestinglevel, beforepathfile, beforestartscope, beforestartline, beforeendline, tool, summary " +
						"FROM refdiffresult ref " +
						"WHERE ref.summary = :summary";
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("summary", summary);
		//query.setParameter("project", project);
		List<Object[]> results = query.list();
		List<RefElement> refElements = preencher(results, project);
		if (refElements.size() > 0)
			return refElements.get(0);
		else
			return null;
	}
	
	@Override
	public synchronized long[] getIntervalEquivBeforeCaller(String revision, String summary, String path) {
		String sql = "select callerline from callerrefdiff where revision = :revision "
				+ "and summary = :summary and callerpath = :path and type = 'before' "
				+ "and simplename = (select distinct beforesimplename from refdiffresult where revision = :revision "
				+ "and summary = :summary limit 1) order by callerline";
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("revision", revision);
		query.setParameter("summary", summary);
		query.setParameter("path", path);
		List<Object> results = query.list();
		
		long[] interval = {0, 0};
		if (results != null && results.size() > 0) {
			interval[0] = Long.parseLong(results.get(0).toString());
			if (results.size() > 1) {
				interval[1] = Long.parseLong(results.get(results.size()-1).toString());
			} else {
				interval[1] = interval[0];
			}
		}
			
		return interval;			
	}
	
	/*@Override
	public synchronized List<Refac> getRefacFix(String project) {
		String sql = "SELECT distinct revision, refactoringtype, entitybefore, entityafter, elementtype, beforestartline, beforeendline, beforestartscope, beforesimplename, beforepathfile, beforenestinglevel " +
						"FROM refdiffresult ref " +
						"WHERE (:project = ref.project " + 
							"AND ref.revisiontype like 'fix%' " +
						      ") " +						
					  "UNION ALL "+
						 "SELECT distinct cal.revision, cal.refactoringtype, ref.entitybefore, cal.entityafter, 'CALLER', cal.callerstartline, cal.callerendline, cal.callerline, cal.simplename, cal.callerpath, cal.nestinglevel " + 
						   "FROM callerrefdiff cal inner join refdiffresult ref on (ref.summary = cal.summary)" + 
							   "WHERE (:project = cal.project " + 
							   	"AND cal.type = 'before' " +
							   	"AND cal.revisiontype like 'fix%')";
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("project", project);
		List<Object[]> results = query.list();
			
		List<Refac> refacs = new ArrayList<Refac>();
		for (Object[] result : results) {		
			Refac refac = new Refac();
			refac.setRevision(Long.parseLong(result[0].toString()));
			refac.setRefactoringtype(result[1].toString());
			refac.setEntitybefore(result[2].toString());
			refac.setEntityafter(result[3].toString());
			refac.setElementtype(result[4].toString());
			refac.setStartline(Long.parseLong(result[5].toString()));
			refac.setEndline(Long.parseLong(result[6].toString()));
			refac.setStartscope(Long.parseLong(result[7].toString()));
			refac.setSimplename(result[8].toString());
			refac.setPathfile(result[9].toString());
			refac.setNestinglevel(Integer.parseInt(result[10].toString()));
			refacs.add(refac);
		}
		return refacs;			
	}*/
	
	/*@Override
	public synchronized List<Object[]> getRefacBic(String path, String revision, int linenumber, int adjustmentindex, String content) {
		String sql = "SELECT revision, refactoringtype, entitybefore, entityafter, elementtype, afterstartline, afterendline, afterstartscope, aftersimplename " +
						"FROM refdiffresult ref " +
						"WHERE (:revision = ref.revision " + 
							//"AND ref.elementtype <> 'Type' " +
							"AND :path = ref.afterpathfile " +
							"AND ref.revisiontype like 'bic%' " + 
							"AND :revision = ref.revision " +
						      ") " +
							"AND " +
						      "( " +
							//"ref.aftercontent like concat('%',trim(substring(:content from 2)),'%') OR " +
							"(refactoringtype IN ('EXTRACT_OPERATION') AND (:linenumber + :adjustmentindex) >= REF.afterstartline AND :linenumber <= REF.afterendline) OR " +
							"(refactoringtype IN ('EXTRACT_INTERFACE') AND :content like concat('%class%',REF.aftersimplename,'%')) OR " +
							"(refactoringtype IN ('EXTRACT_SUPERCLASS') AND :content like concat('%class%',REF.aftersimplename,'%')) OR " +
							"(refactoringtype IN ('INLINE_OPERATION') AND (:linenumber + :adjustmentindex) >= REF.afterstartline AND :linenumber <= REF.afterendline) OR " +
							"(refactoringtype IN ('MOVE_ATTRIBUTE') AND (:linenumber + :adjustmentindex) >= REF.afterstartline AND :linenumber <= REF.afterendline) OR " +	
							"(refactoringtype IN ('MOVE_OPERATION') AND (:linenumber + :adjustmentindex) >= REF.afterstartline AND :linenumber <= REF.afterendline) OR " +	
							"(refactoringtype IN ('MOVE_CLASS') AND (:content like concat('%class%',REF.aftersimplename,'%') OR :content like concat('%interface%',REF.aftersimplename,'%') OR :content like concat('%enum%',REF.aftersimplename,'%'))) OR " +
							"(refactoringtype IN ('PULL_UP_ATTRIBUTE') AND (:linenumber + :adjustmentindex) >= REF.afterstartline AND :linenumber <= REF.afterendline) OR " +
							"(refactoringtype IN ('PULL_UP_OPERATION') AND (:linenumber + :adjustmentindex) >= REF.afterstartline AND :linenumber <= REF.afterendline) OR " +
							"(refactoringtype IN ('PUSH_DOWN_ATTRIBUTE') AND (:linenumber + :adjustmentindex) >= REF.afterstartline AND :linenumber <= REF.afterendline) OR " +
							"(refactoringtype IN ('PUSH_DOWN_OPERATION') AND (:linenumber + :adjustmentindex) >= REF.afterstartline AND :linenumber <= REF.afterendline) OR " +
							"(refactoringtype IN ('RENAME_METHOD') AND :linenumber >= REF.afterstartline AND :linenumber < REF.afterstartscope) OR " +
							"(refactoringtype IN ('RENAME_CLASS') AND (:linenumber >= afterstartline " + 
								"AND (:linenumber <= REF.afterstartscope OR (:linenumber = REF.afterstartscope+1 AND (:content like '%class%' or :content like '%interface%' or :content like '%enum%'))) " +
								")) OR " +
							"(refactoringtype IN ('MOVE_RENAME_CLASS') AND (REF.afternestinglevel = 0 AND :linenumber >= 1 " + 
								"AND (:linenumber <= REF.afterstartscope OR (:linenumber = REF.afterstartscope+1 AND (:content like '%class%' or :content like '%interface%' or :content like '%enum%'))) " +
								")) OR " +
							"(refactoringtype IN ('MOVE_RENAME_CLASS') AND (REF.afternestinglevel >= 0 AND :linenumber >=afterstartline and (:linenumber <= REF.afterstartscope OR (:linenumber = REF.afterstartscope+1 AND (:content like '%class%' or :content like '%interface%' or :content like '%enum%'))) " +
								")) OR " +
							"(refactoringtype IN ('MOVE_RENAME_CLASS', 'RENAME_CLASS') AND (:content like concat('%class%',REF.aftersimplename,'%') OR :content like concat('%interface%',REF.aftersimplename,'%') OR :content like concat('%enum%',REF.aftersimplename,'%')) " + 
								") " +
							   " ) UNION ALL "+
							   "SELECT revision, refactoringtype, '', entityafter, 'CALLER', callerstartline, callerendline, callerline, simplename " + 
							   "FROM callerrefdiff ref " + 
							   "WHERE (:revision = ref.revision " + 
							   	"AND REF.type = 'after' " +
							   	"AND revisiontype like 'bic%' " +
							   	"AND :path = REF.callerpath " +
							   	"AND :linenumber = REF.callerline)";
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("path", path);
		query.setParameter("revision", revision);
		query.setParameter("linenumber", linenumber);
		query.setParameter("adjustmentindex", adjustmentindex);  
		query.setParameter("content", content);
		List<Object[]> result = query.list();
		return result;
	}*/

}
