package org.saiku.service.olap.totals;

import org.olap4j.CellSetAxis;
import org.olap4j.Position;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Member;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AxisInfo {
  public List<Integer>[] levels;
  public final List<String> uniqueLevelNames = new ArrayList<>();
  public int maxDepth;
  public int measuresMember;
  public List<Position> fullPositions;
  public final CellSetAxis axis;

  public AxisInfo( CellSetAxis axis ) {
    this.axis = axis;
    calcAxisInfo( axis );
  }

  private void calcAxisInfo( CellSetAxis axis ) {
    calcAxisInfo( this, axis );
  }

  private void calcAxisInfo( AxisInfo axisInfo, CellSetAxis axis ) {
    final List<Hierarchy> axisHierarchies = axis.getAxisMetaData().getHierarchies();
    final List<Hierarchy> hierarchies = new ArrayList<>();
    boolean hasNullHierarchy = false;
    for (int i = 0; i < axisHierarchies.size(); i++) {
      if (axisHierarchies.get(i) == null) {
        hasNullHierarchy = true;
        break;
      }
    }
    if (hasNullHierarchy && axis.getPositionCount() > 0) {
      for (Member m : axis.getPositions().get(0).getMembers()) {
        if (m.getHierarchy() != null) {
          hierarchies.add(m.getHierarchy());
        }
      }
    } else {
      hierarchies.addAll(axisHierarchies);
    }

    final int hCount = hierarchies.size();
    final List<Integer> levels[] = new List[ hCount ];
    final HashSet<Integer>[][] usedLevels = new HashSet[ hCount ][];
    final int[] maxDepth = new int[ hCount ];

    for ( int i = 0; i < hCount; i++ ) {
      maxDepth[ i ] = -1;
      levels[ i ] = new ArrayList<>();
      if (hierarchies.get( i ) != null) {
        usedLevels[i] = new HashSet[hierarchies.get(i).getLevels().size()];
      } else {
        usedLevels[i] = new HashSet[0];
      }
      for ( int j = 0; j < usedLevels[ i ].length; j++ ) {
        usedLevels[ i ][ j ] = new HashSet<>();
      }
    }
    axisInfo.measuresMember = Integer.MIN_VALUE;

    for ( final Position p : axis.getPositions() ) {
      int mI = 0;
      for ( final Member m : p.getMembers() ) {
        if ( "Measures".equals( m.getDimension().getName() ) ) {
          axisInfo.measuresMember = mI;
        }
        usedLevels[ mI ][ m.getLevel().getDepth() ].add( m.getDepth() );
        mI++;
      }
    }

    for ( int i = 0; i < usedLevels.length; i++ ) {
      for ( int j = 0; j < usedLevels[ i ].length; j++ ) {
        if ( usedLevels[ i ][ j ].size() > 0 ) {
          levels[ i ].add( j );
          axisInfo.uniqueLevelNames.add( hierarchies.get( i ).getLevels().get( j ).getUniqueName() );
        }
      }
    }

    int maxAxisDepth = 0;
    for ( int i = 0; i < hCount; i++ ) {
      maxAxisDepth += levels[ i ].size();
    }
    axisInfo.levels = levels;
    axisInfo.maxDepth = maxAxisDepth;
    findFullPositions( axisInfo, axis );
  }

  private static void findFullPositions( AxisInfo axisInfo, CellSetAxis axis ) {
    axisInfo.fullPositions = new ArrayList<>(axis.getPositionCount());
    List<Integer>[] levels = axisInfo.levels;
    nextpos:
    for ( final Position p : axis.getPositions() ) {
      int mI = 0;
      for ( final Member m : p.getMembers() ) {
        final int maxDepth = levels[ mI ].get( levels[ mI ].size() - 1 );
        if ( m.getDepth() < maxDepth ) {
          continue nextpos;
        }
        mI++;
      }
      axisInfo.fullPositions.add( p );
    }
  }
}