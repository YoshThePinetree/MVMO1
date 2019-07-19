// Main function
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.io.IOException;

public class MVMOmain {
	public static void main (String arg[]) {
		System.out.println("MVMO: Mean Variance Mapping Optimization");
		
		///////////////////////////
		// Initial configuration //
		///////////////////////////
		
		// #Benchmark number #The global minimum 
		// 1:Rosenbrock function constrained with a cubic and a line, f(1.0,1.0)=0
		// 2:Rosenbrock function constrained to a disk, f(1.0,1.0)=0
		// 3:Mishra's Bird function - constrained, f(-3.1302468,-1.5821422)=-106.7645367
		// 4:Simionescu function, f(+-0.84852813,-+0.84852813)=-0.072
		// 5:2-D Rastrigin function, f(0,0)=0
			
		int fnum=2;			// the function number to solve
		int trial=1;		// the number of trials with different random initial
		int ite=1000;		// the number of iterations for a trial
		int pop=100;		// the number of particles
		int rseed=1;		// random seed: MT method			
		int an=50;			// the archive size
		double gpi=0.7;		// the GP selection rate at first
		double gpf=0.2;		// the GP selection rate at last
		double sf=0.95;		// the initial scaling factor
		double af=2.0;		// the asymmetry factor
		int mi=15;			// the first mutation representatives
		int mf=6;			// the final mutation representatives
		
		int PG[]= {fnum,trial,ite,pop};
		double PP1[]= {gpi,gpf,sf,af};
		int PP2[]= {an,mi,mf};
		
		System.out.printf("General Parameters:\n");
		System.out.printf("trial\titeration\tparticles\n");
		System.out.printf("%d\t%d\t\t%d\n",trial,ite,pop);
		System.out.printf("an\tgpi\tgpf\tsf\taf\tmi\tmf\n");
		System.out.printf("%d\t%.2f\t%.2f\t%.2f\t%.2f\t%d\t%d\n",an,gpi,gpf,sf,af,mi,mf);
		
		ObjFunc ObjFuncObject = new ObjFunc();
		System.out.printf("Function:\n");
		ObjFunc.FuncName(fnum);
		
		double ans=ObjFunc.EvalFunc(0, 0, fnum);
		System.out.printf("%f\n",ans);
		
		/////////////////////////////////////////////////
		// Individual Generation & Initial Evaluation  //
		/////////////////////////////////////////////////
		double[][] Frec = new double [ite][trial];
		double[][] X1rec = new double [ite][trial];
		double[][] X2rec = new double [ite][trial];
		Sfmt rnd = new Sfmt(rseed);
		NormVars norm = new NormVars();
		
		double gp;
		int GP;
		int mn1, mn;

		for(int l=0;l<trial;l++){
			
			double[][] X = new double[pop][2];	// the decision variable matrix
			double[][] Xn = new double[pop][2];	// the NORMALIZED decision variable matrix
			double[] F = new double[pop];		// the objective function for the personal best
			double[] Gb = new double[2];		// the global best vector
			double[] xylim=ObjFunc.GetLimit(fnum);
			
			double minfnc=1000;
			int minind=0;
			// Particle generation for their position and velocity
			
			for(int i=0;i<pop;i++) {
				for(int j=0;j<2;j++) {
					if(j==0) {
						X[i][j]=rnd.NextUnif()*(Math.abs(xylim[0])+Math.abs(xylim[1]))-Math.abs(xylim[0]);
					}
					if(j==1) {
						X[i][j]=rnd.NextUnif()*(Math.abs(xylim[2])+Math.abs(xylim[3]))-Math.abs(xylim[2]);
					}
				}
				F[i]=ObjFunc.EvalFunc(X[i][0], X[i][1], fnum);	// variable evaluation
//				System.out.println(F[i]);
				if(minfnc>F[i]) {
					minfnc=F[i];
					minind=i;
					for(int j=0; j<2; j++) {
						Gb[j]=X[minind][j];
					}
				}
			}
			
			
			Xn=norm.Normalize(X, xylim);	// Normalization of the variables between 0 and 1

			
			// restoration of the individuals into the archives
			// archive exists for every individual
			Archive[] arc = new Archive[pop];
			Archive.DevArc(an);
			for(int i=0; i<arc.length; i++){
				arc[i] = new Archive();
				arc[i].fitness[0] = F[i];
				arc[i].variable[0][0] = Xn[i][0];
				arc[i].variable[0][1] = Xn[i][1];
				arc[i].mean[0][0] = Xn[i][0];
				arc[i].mean[0][1] = Xn[i][1];
				arc[i].variance[0][0] = 1;
				arc[i].variance[0][1] = 1;
				arc[i].shape[0][0] = (-1) * Math.log(arc[i].variance[0][0]) * sf;
				arc[i].shape[0][1] = (-1) * Math.log(arc[i].variance[0][1]) * sf;
			}
			
			/////////////////////////////
			//  MVMO solution search   //
			/////////////////////////////
			IndexSort Isort = new IndexSort();
			Integer [] anum = new Integer[an];
			anum=Isort.Ind(F);
//			System.out.println(Arrays.toString(anum));
			
//			System.out.println(F[anum[1]]);	// refer by this manner
			
			for(int k=0; k<ite; k++) {		// MVMO loop start
//				System.out.printf("Iteration %d\n",k+1);
				gp=gpi + ((k+1)*(gpf-gpi)) / (ite);
		        GP=(int) Math.round(pop*gp);                   // A number of the Good Particles
		        int[] q = new int [GP];
		        for (int i=0; i<GP; i++) {
	        		q[i]=rnd.NextInt(GP);	// random index for choosing parents
		        }	        
		        
		        ////////////////////////////////////////////////
		        // Crossover in good and bad population group //
		        ////////////////////////////////////////////////
		        double[][] Xp = new double [pop][2];
		        double[][] Xo = new double [pop][2];
		        double[][] Xm = new double [pop][2];
		        double[][] Xs1 = new double [pop][2];
		        double[][] Xs2 = new double [pop][2];
		        
		        for(int i=0; i<GP; i++) {	// loop for the GOOD solution
	        		for(int j=0; j<2; j++){
	        			Xp[i][j] = arc[anum[i]].variable[0][j];
	        			Xm[i][j] = arc[anum[q[i]]].mean[0][j];
	        			if(Xp[i][j]<Xm[i][j]){
	        				Xs1[i][j] = (arc[anum[i]].shape[0][j]);
	        				Xs2[i][j] = (arc[anum[i]].shape[0][j]) * af;
	        			}
	        			else{
	        				Xs1[i][j] = (arc[anum[i]].shape[0][j]) * af;
	        				Xs2[i][j] = (arc[anum[i]].shape[0][j]);	        				
	        			}
	        			
	        		}
		        }
		        for(int i=GP; i<pop; i++) {	// loop for the BAD solution: Multi-parent crossover
		        	for(int j=0; j<2; j++){
		        		int stop = 1;
		        		double beta = 0, l1= -1;
		        		while(stop==1) {
		        			beta = 2 * (rnd.NextUnif() - (0.5*(1-Math.pow((k+1)/ite,2))));
		        			l1 = Xp[q[rnd.NextInt(GP)]][j] + (beta * Xp[0][j] - Xp[GP-1][j]); 
		        			
		        			if(l1>=0 && l1<=1) {
		        			 break;	
		        			}
		        		}
		        		Xp[i][j]=l1;
		        		
		        		if(Xp[i][j]<Xm[i][j]){
	        				Xs1[i][j] = (arc[anum[i]].shape[0][j]);
	        				Xs2[i][j] = (arc[anum[i]].shape[0][j]) * af;
	        			}
	        			else{
	        				Xs1[i][j] = (arc[anum[i]].shape[0][j]) * af;
	        				Xs2[i][j] = (arc[anum[i]].shape[0][j]);	        				
	        			}
		        		Xm[i][j]=Xp[i][j];
		        		
		        	}
		        }
		        Xo=Xp;	// parents to offsprings
		        
		        ///////////////////////////////////// END OF CROSSOVER
		        
		        ///////////////////////////////////
		        // Mutation via mapping function //
		        ///////////////////////////////////
		        
		        mn1=(int) Math.round((mi) - (((k+1)*(mi-mf))/ite));
	        	mn=(int) Math.round(mf + (rnd.NextUnif()*(mf+mn1-mf)));		// The number of mutated particles
	        	
	        	// nonlinear functions
	        	double hx = 0;
	        	double h0 = 0;
	        	double h1 = 0;
	        	
	        	for(int i=0; i<mn; i++) {		// loop for the mutation
	        		int r1 = rnd.NextInt(pop);
	        		double q1 = rnd.NextUnif();
        			for(int j=0; j<2; j++) {
        				hx = (Xm[r1][j]*(1-Math.exp((-1)*q1*Xs1[r1][j]))) + ((1-Xm[r1][j])*Math.exp((-1)*(1-q1)*Xs2[r1][j]));
        				h0 = ((1-Xm[r1][j])*Math.exp((-1)*Xs2[r1][j]));
        				h1 = (Xm[r1][j]*(1-Math.exp((-1)*Xs1[r1][j]))) + (1-Xm[r1][j]);
        				Xo[r1][j] = Math.abs(hx + ((1-h1+h0)*q1) - h0 );
            			if(Xo[r1][j]>1) {
            				Xo[r1][j]=1;
            			}else if(Xo[r1][j]<0){
            				Xo[r1][j]=0;
            			}
        			}
	        	}
	        	
	        	// rounding the normalized variable
	        	
	        	
	        	
	        	
        		///////////////////////////////////// END OF MUTATION
	        	
		        /*
		        // the position update
				double[][] X1 = new double[pop][2];	// the particle position matrix
				double[][] V1 = new double[pop][2];	// the particle velocity matrix
				double r1, r2;
				
				for(int i=0;i<pop;i++) {
					for(int j=0;j<2;j++) {
						X1[i][j]=X[i][j] + V[i][j];	// position update
						//System.out.printf("%f\t",X1[i][j]);
						//if(j==1) {
						//	System.out.printf("\n");
						//}
						r1=rnd.NextUnif();
						r2=rnd.NextUnif();
						V1[i][j]=w*V[i][j] + c1*r1*(Pb[i][j]-X[i][j]) + c2*r2*(Gb[j]-X[i][j]);	// velocity update
						
						if(V1[i][j]>vlim) {	// velocity limitation
							V1[i][j]=vlim;
						}
					}
					
					F[i]=ObjFunc.EvalFunc(X1[i][0], X1[i][1], fnum);	// particle evaluation
	//				System.out.printf("%f\n",F[i]);
					
					if(minfnc>F[i]) {	// the global best update
						minfnc=F[i];
						minind=i;
						for(int j=0;j<2;j++) {
							Gb[j]=X1[i][j];
						}
					}
					if(Fpb[i]>F[i]) {	// the personal best update
						Fpb[i]=F[i];
						for(int j=0;j<2;j++) {
							Pb[i][j]=X1[i][j];
						}
					}
				}
				
				X=X1;
				V=V1;
				
				System.out.printf("The best fitness: \t");
				System.out.printf("%f\n",minfnc);
				
				Frec[k][l]=minfnc;
				X1rec[k][l]=Gb[0];
				X2rec[k][l]=Gb[1];
			
*/
			}
		}		
		        
		/*
		// write out the objective function value & search log
		String pathname = "C:\\result\\MVMO\\";
		String sufi= ".csv";
		String fnameF = "Fitness";
		String fnameX1 = "x";
		String fnameX2 = "y";
		WriteResult.Output(Frec, ite, trial, pathname + fnameF + sufi);
		WriteResult.Output(X1rec, ite, trial, pathname + fnameX1 + sufi);
		WriteResult.Output(X2rec, ite, trial, pathname + fnameX2 + sufi);
		
		System.out.println(pathname + "test" + sufi); 
		*/
	}

}

