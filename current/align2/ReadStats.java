package align2;

import java.util.ArrayList;

import dna.AminoAcid;

import stream.Read;

import fileIO.TextStreamWriter;


/**
 * @author Brian Bushnell
 * @date Mar 18, 2013
 *
 */
public class ReadStats {
	
	public ReadStats(){this(true);}
		
	public ReadStats(boolean addToList){
		if(addToList){
			synchronized(ReadStats.class){
				objectList.add(this);
			}
		}

		if(COLLECT_QUALITY_STATS){
			qualLength=new long[2][MAXLEN];
			qualSum=new long[2][MAXLEN];
		}else{
			qualLength=null;
			qualSum=null;
		}

		if(COLLECT_MATCH_STATS){
			matchSum=new long[2][MAXLEN];
			delSum=new long[2][MAXLEN];
			insSum=new long[2][MAXLEN];
			subSum=new long[2][MAXLEN];
			nSum=new long[2][MAXLEN];
			clipSum=new long[2][MAXLEN];
			otherSum=new long[2][MAXLEN];
		}else{
			matchSum=null;
			delSum=null;
			insSum=null;
			subSum=null;
			nSum=null;
			clipSum=null;
			otherSum=null;
		}
		
		if(COLLECT_QUALITY_ACCURACY){
			qualMatch=new long[99];
			qualSub=new long[99];
			qualIns=new long[99];
			qualDel=new long[99];
		}else{
			qualMatch=null;
			qualSub=null;
			qualIns=null;
			qualDel=null;
		}

		if(COLLECT_INSERT_STATS){
			insertHist=new LongList(MAXLEN);
		}else{
			insertHist=null;
		}

		if(COLLECT_BASE_STATS){
			baseHist=new LongList[2][5];
			for(int i=0; i<baseHist.length; i++){
				for(int j=0; j<baseHist[i].length; j++){
					baseHist[i][j]=new LongList(400);
				}
			}
		}else{
			baseHist=null;
		}
	}
	
	public static ReadStats mergeAll(){

		if(objectList==null || objectList.isEmpty()){return merged=null;}
		if(objectList.size()==1){return merged=objectList.get(0);}
		
		ReadStats x=new ReadStats(false);
		for(ReadStats rs : objectList){
			if(COLLECT_QUALITY_STATS){
				for(int i=0; i<MAXLEN; i++){
					x.qualLength[0][i]+=rs.qualLength[0][i];
					x.qualLength[1][i]+=rs.qualLength[1][i];
					x.qualSum[0][i]+=rs.qualSum[0][i];
					x.qualSum[1][i]+=rs.qualSum[1][i];
				}
			}
			if(COLLECT_MATCH_STATS){
				for(int i=0; i<MAXLEN; i++){
					x.matchSum[0][i]+=rs.matchSum[0][i];
					x.matchSum[1][i]+=rs.matchSum[1][i];
					x.delSum[0][i]+=rs.delSum[0][i];
					x.delSum[1][i]+=rs.delSum[1][i];
					x.insSum[0][i]+=rs.insSum[0][i];
					x.insSum[1][i]+=rs.insSum[1][i];
					x.subSum[0][i]+=rs.subSum[0][i];
					x.subSum[1][i]+=rs.subSum[1][i];
					x.nSum[0][i]+=rs.nSum[0][i];
					x.nSum[1][i]+=rs.nSum[1][i];
					x.clipSum[0][i]+=rs.clipSum[0][i];
					x.clipSum[1][i]+=rs.clipSum[1][i];
					x.otherSum[0][i]+=rs.otherSum[0][i];
					x.otherSum[1][i]+=rs.otherSum[1][i];
				}
			}
			if(COLLECT_INSERT_STATS){
				x.insertHist.add(rs.insertHist);
			}
			if(COLLECT_BASE_STATS){
				for(int i=0; i<rs.baseHist.length; i++){
					for(int j=0; j<rs.baseHist[i].length; j++){
						x.baseHist[i][j].add(rs.baseHist[i][j]);
					}
				}
			}
			if(COLLECT_QUALITY_ACCURACY){
				for(int i=0; i<x.qualMatch.length; i++){
					x.qualMatch[i]+=rs.qualMatch[i];
					x.qualSub[i]+=rs.qualSub[i];
					x.qualIns[i]+=rs.qualIns[i];
					x.qualDel[i]+=rs.qualDel[i];
				}
			}
			
		}
		
		merged=x;
		return x;
	}
	
	public void addToQualityHistogram(final Read r){
		if(r==null){return;}
		addToQualityHistogram(r, r.obj, 0);
		if(r.mate!=null){addToQualityHistogram(r.mate, r.obj, 1);}
	}
	
	private void addToQualityHistogram(final Read r, Object obj, final int pairnum){
		if(r==null || r.quality==null || r.quality.length<1){return;}
		final byte[] qual;
		if(obj!=null && obj.getClass()==TrimRead.class){
			qual=(pairnum==0 ? ((TrimRead)obj).qual1 : ((TrimRead)obj).qual2);
		}else{
			qual=r.quality;
		}
		final int limit=Tools.min(qual.length, MAXLEN);
		final long[] ql=qualLength[pairnum], qs=qualSum[pairnum];
		ql[limit-1]++;
		for(int i=0; i<limit; i++){qs[i]+=qual[i];}
	}
	
	public void addToQualityAccuracy(final Read r){
		if(r==null || r.quality==null || r.quality.length<1 || !r.mapped() || r.match==null/* || r.discarded()*/){return;}
		final byte[] bases=r.bases;
		final byte[] qual=r.quality;
		final byte[] match=r.match;


		final boolean plus=(r.strand()==0);
		int rpos=0;
		byte lastm='A';
		for(int mpos=0; mpos<match.length/* && rpos<limit*/; mpos++){
			byte b=bases[rpos];
			byte q=qual[rpos];
			byte m=match[plus ? mpos : match.length-mpos-1];
			
			{
				if(m=='m'){
					qualMatch[q]++;
				}else if(m=='S'){
					qualSub[q]++;
				}else if(m=='I'){
					if(AminoAcid.isFullyDefined(b)){qualIns[q]++;}
				}else if(m=='N'){
					//do nothing
				}else if(m=='C'){
					//do nothing
				}else if(m=='D'){
					if(lastm!=m){
						int x=rpos, y=rpos-1;
						if(x<qual.length){
							if(AminoAcid.isFullyDefined(bases[x])){
								qualDel[qual[x]]++;
							}
						}
						if(y>=0){
							if(AminoAcid.isFullyDefined(bases[y])){
								qualDel[qual[y]]++;
							}
						}
					}
					rpos--;
				}else{
					assert(!Character.isDigit(m));
				}
			}

			rpos++;
			lastm=m;
		}
		
	}
	
	public void addToMatchHistogram(final Read r){
		if(r==null){return;}
		addToMatchHistogram(r, 0);
		if(r.mate!=null){addToMatchHistogram(r.mate, 1);}
	}
	
	private void addToMatchHistogram(final Read r, final int pairnum){
		if(r==null || r.bases==null || r.bases.length<1 || !r.mapped() || r.match==null/* || r.discarded()*/){return;}
		final byte[] bases=r.bases, match=r.match;
		final int limit=Tools.min(bases.length, MAXLEN);
		final long[] ms=matchSum[pairnum], ds=delSum[pairnum], is=insSum[pairnum],
				ss=subSum[pairnum], ns=nSum[pairnum], cs=clipSum[pairnum], os=otherSum[pairnum];
		
		if(match==null){
			for(int i=0; i<limit; i++){
				byte b=bases[i];
				if(b=='N'){ns[i]++;}
				else{os[i]++;}
			}
		}else{
			final boolean plus=(r.strand()==0);
			int rpos=0;
			byte lastm='A';
			for(int mpos=0; mpos<match.length && rpos<limit; mpos++){
				byte b=bases[rpos];//bases[plus ? rpos : bases.length-rpos-1];
				byte m=match[plus ? mpos : match.length-mpos-1];//match[mpos];
				if(b=='N'){
					if(m=='D'){
						if(lastm!=m){ds[rpos]++;}
						rpos--;
					}else{ns[rpos]++;}
				}else{
					if(m=='m'){
						ms[rpos]++;
					}else if(m=='S'){
						ss[rpos]++;
					}else if(m=='I'){
						is[rpos]++;
					}else if(m=='N'){
//						assert(false) : "\n"+r+"\n"+new String(Data.getChromosome(r.chrom).getBytes(r.start, r.stop))+"\nrpos="+rpos+", mpos="+mpos;
						os[rpos]++;
					}else if(m=='C'){
//						assert(false) : r;
						cs[rpos]++;
					}else if(m=='D'){
						if(lastm!=m){ds[rpos]++;}
						rpos--;
					}else{
						os[rpos]++;
						assert(false) : "For read "+r.numericID+", unknown symbol in match string: ASCII "+m+" = "+(char)m;
					}
				}
				rpos++;
				lastm=m;
			}
		}
	}
	
	public void addToInsertHistogram(final Read r, boolean ignoreMappingStrand){
		if(verbose){
			System.err.print(r.numericID);
			if(r==null || r.mate==null || !r.mapped() || !r.mate.mapped() || !r.paired()){
				System.err.println("\n");
			}else{
				System.err.println("\t"+r.strand()+"\t"+r.insertSizeMapped(ignoreMappingStrand)+"\t"+r.mate.insertSizeMapped(ignoreMappingStrand));
			}
		}
		if(r==null || r.mate==null || !r.mapped() || !r.mate.mapped() || !r.paired()){return;}
		int x=Tools.min(MAXINSERTLEN, r.insertSizeMapped(ignoreMappingStrand));
		if(x>0){insertHist.increment(x, 1);}
//		assert(x!=1) : "\n"+r+"\n\n"+r.mate+"\n";
//		System.out.println("Incrementing "+x);
	}
	
	public void addToBaseHistogram(final Read r){
		addToBaseHistogram(r, 0);
		if(r.mate!=null){addToBaseHistogram(r.mate, 1);}
	}
	
	public void addToBaseHistogram(final Read r, final int pairnum){
		if(r==null || r.bases==null){return;}
		
		final byte[] bases=r.bases;
		final LongList[] lists=baseHist[pairnum];
		for(int i=0; i<bases.length; i++){
			byte b=bases[i];
			int x=AminoAcid.baseToNumber[b]+1;
			lists[x].increment(i, 1);
		}
	}
	
	public static boolean writeAll(boolean paired){
		if(collectingStats()){
			ReadStats rs=mergeAll();
			if(QUAL_HIST_FILE!=null){rs.writeQualityToFile(QUAL_HIST_FILE, paired);}
			if(MATCH_HIST_FILE!=null){rs.writeMatchToFile(MATCH_HIST_FILE, paired);}
			if(INSERT_HIST_FILE!=null){rs.writeInsertToFile(INSERT_HIST_FILE);}
			if(BASE_HIST_FILE!=null){rs.writeBaseContentToFile(BASE_HIST_FILE, paired);}
			if(QUAL_ACCURACY_FILE!=null){rs.writeQualityAccuracyToFile(QUAL_ACCURACY_FILE);}
			return rs.errorState;
		}
		return false;
	}
	
	public void writeQualityToFile(String fname, boolean writePaired){
		TextStreamWriter tsw=new TextStreamWriter(fname, overwrite, append, false);
		tsw.start();
		tsw.print("#BaseNum\tRead1"+(writePaired ? "\tRead2" : "")+"\n");
		
		final long[] qs1=qualSum[0], qs2=qualSum[1], ql1=qualLength[0], ql2=qualLength[1];
		
		for(int i=MAXLEN-2; i>=0; i--){
			ql1[i]+=ql1[i+1];
			ql2[i]+=ql2[i+1];
		}
		
		if(writePaired){
			for(int i=0; i<MAXLEN && (ql1[i]>0 || ql2[i]>0); i++){
				int a=i+1;
				double b=qs1[i]/(double)Tools.max(1, ql1[i]);
				double c=qs2[i]/(double)Tools.max(1, ql2[i]);
				tsw.print(String.format("%d\t%.3f\t%.3f\n", a, b, c));
			}
		}else{
			for(int i=0; i<MAXLEN && ql1[i]>0; i++){
				int a=i+1;
				double b=qs1[i]/(double)Tools.max(1, ql1[i]);
				tsw.print(String.format("%d\t%.3f\n", a, b));
			}
		}
		tsw.poison();
		tsw.waitForFinish();
		errorState|=tsw.errorState;
	}
	
	public void writeQualityAccuracyToFile(String fname){
		TextStreamWriter tsw=new TextStreamWriter(fname, overwrite, append, false);
		tsw.start();
		tsw.print("#Quality\tMatch\tSub\tIns\tDel\tTrueQuality\tTrueQualitySub\n");
		
		int max=qualMatch.length;
		for(int i=max-1; i>=0; i--){
			if(qualMatch[i]+qualSub[i]+qualIns[i]+qualDel[i]>0){break;}
			max=i;
		}
		
		for(int i=0; i<max; i++){
			long qm=qualMatch[i]*2;
			long qs=qualSub[i]*2;
			long qi=qualIns[i]*2;
			long qd=qualDel[i];
			
			double phred=-1;
			double phredSub=-1;
			
			long sum=qm+qs+qi+qd;
			if(sum>0){
				double mult=1.0/sum;
				double subRate=(qs)*mult;
				double errorRate=(qs+qi+qd)*mult;
				
				phredSub=QualityTools.probErrorToPhredDouble(subRate);
				phred=QualityTools.probErrorToPhredDouble(errorRate);
				
//				System.err.println("sub: "+qs+"/"+sum+" -> "+subRate+" -> "+phredSub);
			}
			
			tsw.print(i+"\t"+qm+"\t"+qs+"\t"+qi+"\t"+qd);
			tsw.print(phred>=0 ? String.format("\t%.2f", phred) : "\t");
			tsw.print(phredSub>=0 ? String.format("\t%.2f\n", phredSub) : "\t\n");
			
//			System.err.println(qm+"\t"+qs+"\t"+qi+"\t"+qd);
		}
		
		tsw.poison();
		tsw.waitForFinish();
		errorState|=tsw.errorState;
	}
	
	public void writeMatchToFile(String fname, boolean writePaired){
		if(!writePaired){
			writeMatchToFileUnpaired(fname);
			return;
		}
		TextStreamWriter tsw=new TextStreamWriter(fname, overwrite, false, false);
		tsw.start();
		tsw.print("#BaseNum\tMatch1\tSub1\tDel1\tIns1\tN1\tOther1\tMatch2\tSub2\tDel2\tIns2\tN2\tOther2\n");
		
		final long[] ms1=matchSum[0], ds1=delSum[0], is1=insSum[0],
				ss1=subSum[0], ns1=nSum[0], cs1=clipSum[0], os1=otherSum[0];
		final long[] ms2=matchSum[1], ds2=delSum[1], is2=insSum[1],
				ss2=subSum[1], ns2=nSum[1], cs2=clipSum[1], os2=otherSum[1];
		
		for(int i=0; i<MAXLEN; i++){
			int a=i+1;
			long sum1=ms1[i]+is1[i]+ss1[i]+ns1[i]+cs1[i]+os1[i]; //no deletions
			long sum2=ms2[i]+is2[i]+ss2[i]+ns2[i]+cs2[i]+os2[i]; //no deletions
			if(sum1==0 && sum2==0){break;}
			double inv1=1.0/(double)Tools.max(1, sum1);
			double inv2=1.0/(double)Tools.max(1, sum2);

			tsw.print(String.format("%d", a));
			tsw.print(String.format("\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f", 
					ms1[i]*inv1, ss1[i]*inv1, ds1[i]*inv1, is1[i]*inv1, ns1[i]*inv1, (os1[i]+cs1[i])*inv1));
			tsw.print(String.format("\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f", 
					ms2[i]*inv2, ss2[i]*inv2, ds2[i]*inv2, is2[i]*inv2, ns2[i]*inv2, (os2[i]+cs2[i])*inv2)
//					+", "+ms2[i]+", "+is2[i]+", "+ss2[i]+", "+ns2[i]+", "+cs2[i]+", "+os2[i]
					);
			tsw.print("\n");
		}
		tsw.poison();
		tsw.waitForFinish();
		errorState|=tsw.errorState;
	}
	
	public void writeMatchToFileUnpaired(String fname){
		TextStreamWriter tsw=new TextStreamWriter(fname, overwrite, false, false);
		tsw.start();
		tsw.print("#BaseNum\tMatch1\tSub1\tDel1\tIns1\tN1\tOther1\n");
		
		final long[] ms1=matchSum[0], ds1=delSum[0], is1=insSum[0],
				ss1=subSum[0], ns1=nSum[0], cs1=clipSum[0], os1=otherSum[0];
		
		for(int i=0; i<MAXLEN; i++){
			int a=i+1;
			long sum1=ms1[i]+is1[i]+ss1[i]+ns1[i]+cs1[i]+os1[i]; //no deletions
			if(sum1==0){break;}
			double inv1=1.0/(double)Tools.max(1, sum1);

			tsw.print(String.format("%d", a));
			tsw.print(String.format("\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f", 
					ms1[i]*inv1, ss1[i]*inv1, ds1[i]*inv1, is1[i]*inv1, ns1[i]*inv1, (os1[i]+cs1[i])*inv1)
//					+", "+ms1[i]+", "+is1[i]+", "+ss1[i]+", "+ns1[i]+", "+cs1[i]+", "+os1[i]
					);
			tsw.print("\n");
		}
		tsw.poison();
		tsw.waitForFinish();
		errorState|=tsw.errorState;
	}
	
	public void writeInsertToFile(String fname){
		TextStreamWriter tsw=new TextStreamWriter(fname, overwrite, false, false);
		tsw.start();
		tsw.print("#InsertSize\tCount\n");
		
		for(int i=0; i<insertHist.size; i++){
			long x=insertHist.get(i);
			if(x>0 || !skipZeroInsertCount){
				tsw.print(i+"\t"+x+"\t"+"\n");
			}
		}
		tsw.poison();
		tsw.waitForFinish();
		errorState|=tsw.errorState;
	}
	
	public void writeBaseContentToFile(String fname, boolean paired){
		TextStreamWriter tsw=new TextStreamWriter(fname, overwrite, false, false);
		tsw.start();
		if(paired){
			tsw.print("#Pos\tA\tC\tG\tT\tN\n");
		}
		
		LongList[] lists;
		
		int max=writeBaseContentToFile2(tsw, baseHist[0], 0);
		if(paired){
			writeBaseContentToFile2(tsw, baseHist[1], max);
		}
		
		tsw.poison();
		tsw.waitForFinish();
		errorState|=tsw.errorState;
	}
	
	private static int writeBaseContentToFile2(TextStreamWriter tsw, LongList[] lists, int offset){
		int max=0;
		StringBuilder sb=new StringBuilder(100);
		for(LongList ll : lists){max=Tools.max(max, ll.size);}
		for(int i=0; i<max; i++){
			long a=lists[1].get(i);
			long c=lists[2].get(i);
			long g=lists[3].get(i);
			long t=lists[4].get(i);
			long n=lists[0].get(i);
			double mult=1.0/(a+c+g+t+n);

			sb.setLength(0);
			sb.append(i+offset).append('\t');
			sb.append(String.format("%.5f\t", a*mult));
			sb.append(String.format("%.5f\t", c*mult));
			sb.append(String.format("%.5f\t", g*mult));
			sb.append(String.format("%.5f\t", t*mult));
			sb.append(String.format("%.5f\n", n*mult));
			
			tsw.print(sb.toString());
		}
		return max;
	}
	
	public static boolean collectingStats(){
		return COLLECT_QUALITY_STATS || COLLECT_QUALITY_ACCURACY || COLLECT_MATCH_STATS || COLLECT_INSERT_STATS || COLLECT_BASE_STATS;
	}
	
	public final long[][] qualLength;
	public final long[][] qualSum;
	
	public final long[][] matchSum;
	public final long[][] delSum;
	public final long[][] insSum;
	public final long[][] subSum;
	public final long[][] nSum;
	public final long[][] clipSum;
	public final long[][] otherSum;

	public final long[] qualMatch;
	public final long[] qualSub;
	public final long[] qualIns;
	public final long[] qualDel;

	public final LongList[][] baseHist;
	
	public final LongList insertHist;

	public static final int MAXLEN=2000;
	public static final int MAXINSERTLEN=24000;
	
	public boolean errorState=false;
	
	public static ReadStats merged=null;
	
	public static ArrayList<ReadStats> objectList=new ArrayList<ReadStats>();
	public static boolean COLLECT_QUALITY_STATS=false;
	public static boolean COLLECT_QUALITY_ACCURACY=false;
	public static boolean COLLECT_MATCH_STATS=false;
	public static boolean COLLECT_INSERT_STATS=false;
	public static boolean COLLECT_BASE_STATS=false;
	public static String QUAL_HIST_FILE=null;
	public static String QUAL_ACCURACY_FILE=null;
	public static String MATCH_HIST_FILE=null;
	public static String INSERT_HIST_FILE=null;
	public static String BASE_HIST_FILE=null;
	public static boolean overwrite=false;
	public static boolean append=false;
	public static final boolean verbose=false;
	
	public static boolean skipZeroInsertCount=true;
	
}